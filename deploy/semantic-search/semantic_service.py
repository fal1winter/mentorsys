#!/usr/bin/env python3
"""
语义检索服务 - 使用 Milvus Lite + Sentence Transformers
提供论文和笔记的语义检索能力
支持 RabbitMQ 消息队列消费
"""

import os
import json
import logging

# 设置 HuggingFace 镜像源（必须在导入 sentence_transformers 之前）
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
from pymilvus import MilvusClient, DataType
from rabbitmq_consumer import start_consumer_thread

# 配置日志
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)

# 配置
MILVUS_URI = "http://127.0.0.1:19530"
EMBEDDING_MODEL = "BAAI/bge-small-zh-v1.5"  # 中文效果好，轻量级
EMBEDDING_DIM = 512
PAPER_COLLECTION = "papers"
NOTE_COLLECTION = "notes"
MENTOR_COLLECTION = "mentor_profiles"
STUDENT_COLLECTION = "student_profiles"

# 全局变量
model = None
milvus_client = None

def init_model():
    """初始化 embedding 模型"""
    global model
    logger.info(f"Loading embedding model: {EMBEDDING_MODEL}")
    model = SentenceTransformer(EMBEDDING_MODEL)
    logger.info("Model loaded successfully")

def init_milvus():
    """初始化 Milvus Lite"""
    global milvus_client
    milvus_client = MilvusClient(uri=MILVUS_URI)
    logger.info(f"Milvus Standalone connected at {MILVUS_URI}")
    
    # 创建论文集合
    if not milvus_client.has_collection(PAPER_COLLECTION):
        milvus_client.create_collection(
            collection_name=PAPER_COLLECTION,
            dimension=EMBEDDING_DIM,
            metric_type="COSINE",
            auto_id=False,
            id_type=DataType.INT64
        )
        logger.info(f"Created collection: {PAPER_COLLECTION}")
    
    # 创建笔记集合
    if not milvus_client.has_collection(NOTE_COLLECTION):
        milvus_client.create_collection(
            collection_name=NOTE_COLLECTION,
            dimension=EMBEDDING_DIM,
            metric_type="COSINE",
            auto_id=False,
            id_type=DataType.INT64
        )
        logger.info(f"Created collection: {NOTE_COLLECTION}")

    # 创建导师档案集合
    if not milvus_client.has_collection(MENTOR_COLLECTION):
        milvus_client.create_collection(
            collection_name=MENTOR_COLLECTION,
            dimension=EMBEDDING_DIM,
            metric_type="COSINE",
            auto_id=False,
            id_type=DataType.INT64
        )
        logger.info(f"Created collection: {MENTOR_COLLECTION}")

    # 创建学生档案集合
    if not milvus_client.has_collection(STUDENT_COLLECTION):
        milvus_client.create_collection(
            collection_name=STUDENT_COLLECTION,
            dimension=EMBEDDING_DIM,
            metric_type="COSINE",
            auto_id=False,
            id_type=DataType.INT64
        )
        logger.info(f"Created collection: {STUDENT_COLLECTION}")

def get_embedding(text):
    """获取文本的 embedding 向量"""
    if not text:
        return [0.0] * EMBEDDING_DIM
    return model.encode(text, normalize_embeddings=True).tolist()


@app.route('/health', methods=['GET'])
def health():
    """健康检查"""
    return jsonify({"status": "ok", "model": EMBEDDING_MODEL})

@app.route('/embedding', methods=['POST'])
def embedding():
    """获取文本的 embedding 向量"""
    data = request.json
    text = data.get('text', '')
    vector = get_embedding(text)
    return jsonify({"vector": vector, "dimension": len(vector)})

@app.route('/paper/index', methods=['POST'])
def index_paper():
    """索引论文"""
    data = request.json
    paper_id = data.get('id')
    title = data.get('title', '')
    abstract_text = data.get('abstractText', '')
    keywords = data.get('keywords', [])
    authors = data.get('authors', [])
    
    # 组合文本用于 embedding
    combined_text = f"{title} {abstract_text} {' '.join(keywords) if keywords else ''} {' '.join(authors) if authors else ''}"
    vector = get_embedding(combined_text)
    
    # 存储元数据
    metadata = {
        "title": title[:500] if title else "",
        "abstract": abstract_text[:1000] if abstract_text else "",
        "keywords": json.dumps(keywords) if keywords else "[]"
    }
    
    try:
        # 先删除已存在的
        milvus_client.delete(collection_name=PAPER_COLLECTION, ids=[paper_id])
    except:
        pass
    
    # 插入新数据
    milvus_client.insert(
        collection_name=PAPER_COLLECTION,
        data=[{"id": paper_id, "vector": vector, **metadata}]
    )
    
    return jsonify({"success": True, "id": paper_id})

@app.route('/paper/batch_index', methods=['POST'])
def batch_index_papers():
    """批量索引论文"""
    papers = request.json.get('papers', [])
    indexed = 0
    
    for paper in papers:
        paper_id = paper.get('id')
        title = paper.get('title', '')
        abstract_text = paper.get('abstractText', '')
        keywords = paper.get('keywords', [])
        authors = paper.get('authors', [])
        
        combined_text = f"{title} {abstract_text} {' '.join(keywords) if keywords else ''} {' '.join(authors) if authors else ''}"
        vector = get_embedding(combined_text)
        
        metadata = {
            "title": title[:500] if title else "",
            "abstract": abstract_text[:1000] if abstract_text else "",
            "keywords": json.dumps(keywords) if keywords else "[]"
        }
        
        try:
            milvus_client.delete(collection_name=PAPER_COLLECTION, ids=[paper_id])
        except:
            pass
        
        milvus_client.insert(
            collection_name=PAPER_COLLECTION,
            data=[{"id": paper_id, "vector": vector, **metadata}]
        )
        indexed += 1
    
    return jsonify({"success": True, "indexed": indexed})

@app.route('/paper/search', methods=['POST'])
def search_papers():
    """语义搜索论文"""
    data = request.json
    query = data.get('query', '')
    top_k = data.get('topK', 10)
    
    if not query:
        return jsonify({"results": []})
    
    query_vector = get_embedding(query)
    
    results = milvus_client.search(
        collection_name=PAPER_COLLECTION,
        data=[query_vector],
        limit=top_k,
        output_fields=["title", "abstract", "keywords"]
    )
    
    search_results = []
    for hits in results:
        for hit in hits:
            search_results.append({
                "id": hit["id"],
                "score": float(hit["distance"]),
                "title": hit["entity"].get("title", ""),
                "abstract": hit["entity"].get("abstract", ""),
                "keywords": json.loads(hit["entity"].get("keywords", "[]"))
            })
    
    return jsonify({"results": search_results})


@app.route('/note/index', methods=['POST'])
def index_note():
    """索引阅读笔记"""
    data = request.json
    note_id = data.get('id')
    paper_id = data.get('paperId')
    paper_title = data.get('paperTitle', '')
    summary = data.get('summary', '')
    note_content = data.get('noteContent', '')
    
    # 组合文本
    combined_text = f"{paper_title} {summary} {note_content[:2000] if note_content else ''}"
    vector = get_embedding(combined_text)
    
    metadata = {
        "paper_id": paper_id,
        "paper_title": paper_title[:500] if paper_title else "",
        "summary": summary[:1000] if summary else ""
    }
    
    try:
        milvus_client.delete(collection_name=NOTE_COLLECTION, ids=[note_id])
    except:
        pass
    
    milvus_client.insert(
        collection_name=NOTE_COLLECTION,
        data=[{"id": note_id, "vector": vector, **metadata}]
    )
    
    return jsonify({"success": True, "id": note_id})

@app.route('/note/batch_index', methods=['POST'])
def batch_index_notes():
    """批量索引笔记"""
    notes = request.json.get('notes', [])
    indexed = 0
    
    for note in notes:
        note_id = note.get('id')
        paper_id = note.get('paperId')
        paper_title = note.get('paperTitle', '')
        summary = note.get('summary', '')
        note_content = note.get('noteContent', '')
        
        combined_text = f"{paper_title} {summary} {note_content[:2000] if note_content else ''}"
        vector = get_embedding(combined_text)
        
        metadata = {
            "paper_id": paper_id,
            "paper_title": paper_title[:500] if paper_title else "",
            "summary": summary[:1000] if summary else ""
        }
        
        try:
            milvus_client.delete(collection_name=NOTE_COLLECTION, ids=[note_id])
        except:
            pass
        
        milvus_client.insert(
            collection_name=NOTE_COLLECTION,
            data=[{"id": note_id, "vector": vector, **metadata}]
        )
        indexed += 1
    
    return jsonify({"success": True, "indexed": indexed})

@app.route('/note/search', methods=['POST'])
def search_notes():
    """语义搜索笔记"""
    data = request.json
    query = data.get('query', '')
    top_k = data.get('topK', 10)
    
    if not query:
        return jsonify({"results": []})
    
    query_vector = get_embedding(query)
    
    results = milvus_client.search(
        collection_name=NOTE_COLLECTION,
        data=[query_vector],
        limit=top_k,
        output_fields=["paper_id", "paper_title", "summary"]
    )
    
    search_results = []
    for hits in results:
        for hit in hits:
            search_results.append({
                "id": hit["id"],
                "score": float(hit["distance"]),
                "paperId": hit["entity"].get("paper_id"),
                "paperTitle": hit["entity"].get("paper_title", ""),
                "summary": hit["entity"].get("summary", "")
            })
    
    return jsonify({"results": search_results})

@app.route('/search', methods=['POST'])
def unified_search():
    """统一语义搜索（同时搜索论文和笔记）"""
    data = request.json
    query = data.get('query', '')
    top_k = data.get('topK', 10)
    search_type = data.get('type', 'all')  # all, paper, note
    
    if not query:
        return jsonify({"papers": [], "notes": []})
    
    query_vector = get_embedding(query)
    result = {"papers": [], "notes": []}
    
    # 搜索论文
    if search_type in ['all', 'paper']:
        paper_results = milvus_client.search(
            collection_name=PAPER_COLLECTION,
            data=[query_vector],
            limit=top_k,
            output_fields=["title", "abstract", "keywords"]
        )
        for hits in paper_results:
            for hit in hits:
                result["papers"].append({
                    "id": hit["id"],
                    "score": float(hit["distance"]),
                    "title": hit["entity"].get("title", ""),
                    "abstract": hit["entity"].get("abstract", ""),
                    "keywords": json.loads(hit["entity"].get("keywords", "[]"))
                })
    
    # 搜索笔记
    if search_type in ['all', 'note']:
        note_results = milvus_client.search(
            collection_name=NOTE_COLLECTION,
            data=[query_vector],
            limit=top_k,
            output_fields=["paper_id", "paper_title", "summary"]
        )
        for hits in note_results:
            for hit in hits:
                result["notes"].append({
                    "id": hit["id"],
                    "score": float(hit["distance"]),
                    "paperId": hit["entity"].get("paper_id"),
                    "paperTitle": hit["entity"].get("paper_title", ""),
                    "summary": hit["entity"].get("summary", "")
                })
    
    return jsonify(result)

@app.route('/paper/similar', methods=['POST'])
def similar_papers():
    """获取相似论文 - 基于论文ID查找语义相似的论文"""
    data = request.json
    paper_id = data.get('paperId')
    top_k = data.get('topK', 10)
    
    if not paper_id:
        return jsonify({"results": [], "error": "paperId is required"})
    
    try:
        # 先获取该论文的向量
        paper_data = milvus_client.get(
            collection_name=PAPER_COLLECTION,
            ids=[paper_id],
            output_fields=["vector"]
        )
        
        if not paper_data or len(paper_data) == 0:
            # 如果论文不在索引中，返回空结果
            return jsonify({"results": [], "error": "paper not indexed"})
        
        # 使用该论文的向量搜索相似论文
        paper_vector = paper_data[0].get("vector")
        if not paper_vector:
            return jsonify({"results": [], "error": "paper vector not found"})
        
        # 搜索相似论文（多取一些，因为要排除自己）
        results = milvus_client.search(
            collection_name=PAPER_COLLECTION,
            data=[paper_vector],
            limit=top_k + 1,
            output_fields=["title", "abstract", "keywords"]
        )
        
        search_results = []
        for hits in results:
            for hit in hits:
                # 排除自己
                if hit["id"] == paper_id:
                    continue
                search_results.append({
                    "id": hit["id"],
                    "score": float(hit["distance"]),
                    "title": hit["entity"].get("title", ""),
                    "abstract": hit["entity"].get("abstract", ""),
                    "keywords": json.loads(hit["entity"].get("keywords", "[]"))
                })
        
        # 只返回 top_k 个结果
        return jsonify({"results": search_results[:top_k]})
    except Exception as e:
        logger.error(f"Error finding similar papers: {e}")
        return jsonify({"results": [], "error": str(e)})


@app.route('/paper/similar_by_text', methods=['POST'])
def similar_papers_by_text():
    """基于文本查找相似论文 - 用于没有索引的论文"""
    data = request.json
    title = data.get('title', '')
    abstract_text = data.get('abstractText', '')
    keywords = data.get('keywords', [])
    exclude_id = data.get('excludeId')  # 要排除的论文ID
    top_k = data.get('topK', 10)
    
    # 组合文本
    combined_text = f"{title} {abstract_text} {' '.join(keywords) if keywords else ''}"
    if not combined_text.strip():
        return jsonify({"results": []})
    
    query_vector = get_embedding(combined_text)
    
    results = milvus_client.search(
        collection_name=PAPER_COLLECTION,
        data=[query_vector],
        limit=top_k + 1,
        output_fields=["title", "abstract", "keywords"]
    )
    
    search_results = []
    for hits in results:
        for hit in hits:
            # 排除指定的论文
            if exclude_id and hit["id"] == exclude_id:
                continue
            search_results.append({
                "id": hit["id"],
                "score": float(hit["distance"]),
                "title": hit["entity"].get("title", ""),
                "abstract": hit["entity"].get("abstract", ""),
                "keywords": json.loads(hit["entity"].get("keywords", "[]"))
            })
    
    return jsonify({"results": search_results[:top_k]})


@app.route('/paper/delete', methods=['POST'])
def delete_paper():
    """删除论文索引"""
    paper_id = request.json.get('id')
    try:
        milvus_client.delete(collection_name=PAPER_COLLECTION, ids=[paper_id])
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)})

@app.route('/note/delete', methods=['POST'])
def delete_note():
    """删除笔记索引"""
    note_id = request.json.get('id')
    try:
        milvus_client.delete(collection_name=NOTE_COLLECTION, ids=[note_id])
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)})

@app.route('/stats', methods=['GET'])
def stats():
    """获取索引统计信息"""
    paper_count = milvus_client.get_collection_stats(PAPER_COLLECTION)
    note_count = milvus_client.get_collection_stats(NOTE_COLLECTION)
    mentor_count = milvus_client.get_collection_stats(MENTOR_COLLECTION)
    student_count = milvus_client.get_collection_stats(STUDENT_COLLECTION)
    return jsonify({
        "papers": paper_count,
        "notes": note_count,
        "mentors": mentor_count,
        "students": student_count
    })

# ============================================
# MENTOR PROFILE APIs
# ============================================

@app.route('/mentor/index', methods=['POST'])
def index_mentor():
    """索引导师档案"""
    data = request.json
    mentor_id = data.get('id')
    name = data.get('name', '')
    research_areas = data.get('researchAreas', '')
    bio = data.get('bio', '')
    institution = data.get('institution', '')
    title = data.get('title', '')

    # 组合文本用于 embedding
    combined_text = f"{name} {title} {institution} {research_areas} {bio}"
    vector = get_embedding(combined_text)

    # 存储元数据
    metadata = {
        "name": name[:200] if name else "",
        "research_areas": research_areas[:500] if research_areas else "",
        "institution": institution[:200] if institution else ""
    }

    try:
        # 先删除已存在的
        milvus_client.delete(collection_name=MENTOR_COLLECTION, ids=[mentor_id])
    except:
        pass

    # 插入新数据
    milvus_client.insert(
        collection_name=MENTOR_COLLECTION,
        data=[{"id": mentor_id, "vector": vector, **metadata}]
    )

    return jsonify({"success": True, "id": mentor_id})

@app.route('/mentor/search', methods=['POST'])
def search_mentors():
    """语义搜索导师档案"""
    data = request.json
    query = data.get('query', '')
    top_k = data.get('topK', 10)

    if not query:
        return jsonify({"results": []})

    query_vector = get_embedding(query)

    results = milvus_client.search(
        collection_name=MENTOR_COLLECTION,
        data=[query_vector],
        limit=top_k,
        output_fields=["name", "research_areas", "institution"]
    )

    search_results = []
    for hits in results:
        for hit in hits:
            search_results.append({
                "id": hit["id"],
                "score": float(hit["distance"]),
                "name": hit["entity"].get("name", ""),
                "researchAreas": hit["entity"].get("research_areas", ""),
                "institution": hit["entity"].get("institution", "")
            })

    return jsonify({"results": search_results})

@app.route('/mentor/delete', methods=['POST'])
def delete_mentor():
    """删除导师档案索引"""
    mentor_id = request.json.get('id')
    try:
        milvus_client.delete(collection_name=MENTOR_COLLECTION, ids=[mentor_id])
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)})

# ============================================
# STUDENT PROFILE APIs
# ============================================

@app.route('/student/index', methods=['POST'])
def index_student():
    """索引学生档案"""
    data = request.json
    student_id = data.get('id')
    name = data.get('name', '')
    research_interests = data.get('researchInterests', '')
    bio = data.get('bio', '')
    institution = data.get('institution', '')
    major = data.get('major', '')

    # 组合文本用于 embedding
    combined_text = f"{name} {major} {institution} {research_interests} {bio}"
    vector = get_embedding(combined_text)

    # 存储元数据
    metadata = {
        "name": name[:200] if name else "",
        "research_interests": research_interests[:500] if research_interests else "",
        "institution": institution[:200] if institution else ""
    }

    try:
        # 先删除已存在的
        milvus_client.delete(collection_name=STUDENT_COLLECTION, ids=[student_id])
    except:
        pass

    # 插入新数据
    milvus_client.insert(
        collection_name=STUDENT_COLLECTION,
        data=[{"id": student_id, "vector": vector, **metadata}]
    )

    return jsonify({"success": True, "id": student_id})

@app.route('/student/search', methods=['POST'])
def search_students():
    """语义搜索学生档案"""
    data = request.json
    query = data.get('query', '')
    top_k = data.get('topK', 10)

    if not query:
        return jsonify({"results": []})

    query_vector = get_embedding(query)

    results = milvus_client.search(
        collection_name=STUDENT_COLLECTION,
        data=[query_vector],
        limit=top_k,
        output_fields=["name", "research_interests", "institution"]
    )

    search_results = []
    for hits in results:
        for hit in hits:
            search_results.append({
                "id": hit["id"],
                "score": float(hit["distance"]),
                "name": hit["entity"].get("name", ""),
                "researchInterests": hit["entity"].get("research_interests", ""),
                "institution": hit["entity"].get("institution", "")
            })

    return jsonify({"results": search_results})

@app.route('/student/delete', methods=['POST'])
def delete_student():
    """删除学生档案索引"""
    student_id = request.json.get('id')
    try:
        milvus_client.delete(collection_name=STUDENT_COLLECTION, ids=[student_id])
        return jsonify({"success": True})
    except Exception as e:
        return jsonify({"success": False, "error": str(e)})

if __name__ == '__main__':
    init_model()
    init_milvus()

    # 启动 RabbitMQ 消费者（后台线程）
    logger.info("Starting RabbitMQ consumer...")
    try:
        consumer = start_consumer_thread(milvus_client, model)
        logger.info("RabbitMQ consumer started successfully")
    except Exception as e:
        logger.error(f"Failed to start RabbitMQ consumer: {e}")
        logger.info("Service will continue without RabbitMQ consumer")

    logger.info("Starting Flask HTTP service on port 5050...")
    app.run(host='0.0.0.0', port=5050, debug=False)
