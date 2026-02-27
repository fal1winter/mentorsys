# 增强版推荐系统设计文档

## 1. 系统概述

### 1.1 当前系统分析

**现有功能：**
- 基于用户浏览历史的推荐
- 使用LLM分析用户偏好（研究方向、关键词）
- 简单的关键词匹配推荐
- 热门导师推荐作为后备

**现有问题：**
- 只考虑研究方向匹配，未考虑其他重要因素
- 没有使用向量数据库进行语义相似度搜索
- 缺少导师侧的推荐（只有学生找导师）
- 推荐理由单一，缺乏多维度分析

### 1.2 增强目标

根据用户需求，增强推荐系统需要：

1. **使用Milvus向量数据库**：存储导师和学生的特征向量，进行高效的语义相似度搜索
2. **多维度分析**：综合考虑以下因素
   - 研究方向匹配度
   - 人品要求（通过评价分析）
   - 组内工作强度（通过学生反馈）
   - 能力要求（GPA、学历、背景）
   - 导师接收能力（当前学生数/最大学生数）
3. **双向推荐**：
   - 学生侧：推荐合适的导师
   - 导师侧：推荐合适的学生
4. **LLM深度分析**：使用LLM分析导师和学生的详细资料，生成综合评估

## 2. 技术架构

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端界面                              │
│  ┌──────────────┐              ┌──────────────┐            │
│  │ 学生推荐页面  │              │ 导师推荐页面  │            │
│  └──────────────┘              └──────────────┘            │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                   RecommendationController                   │
│  /recommendations/mentors (学生获取导师推荐)                  │
│  /recommendations/students (导师获取学生推荐)                 │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              EnhancedRecommendationService                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1. 获取用户画像（从MySQL）                            │   │
│  │ 2. 生成特征向量（通过LLM Embedding）                  │   │
│  │ 3. 向量相似度搜索（Milvus）                           │   │
│  │ 4. 多维度评分计算                                     │   │
│  │ 5. LLM生成推荐理由                                    │   │
│  │ 6. 排序并返回Top-N推荐                                │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
            │                │                │
            ▼                ▼                ▼
    ┌──────────┐    ┌──────────────┐   ┌──────────┐
    │  MySQL   │    │    Milvus    │   │ LLM API  │
    │(用户数据) │    │  (向量存储)   │   │(分析服务) │
    └──────────┘    └──────────────┘   └──────────┘
```

### 2.2 核心组件

#### 2.2.1 Milvus向量数据库

**Collection设计：**

```python
# mentor_profiles collection
{
    "id": int,                    # 导师ID
    "embedding": vector(1536),    # 特征向量（OpenAI embedding维度）
    "research_areas": string,     # 研究方向
    "institution": string,        # 机构
    "rating_avg": float,          # 平均评分
    "accepting_students": bool,   # 是否接收学生
    "current_students": int,      # 当前学生数
    "max_students": int,          # 最大学生数
    "update_time": timestamp      # 更新时间
}

# student_profiles collection
{
    "id": int,                    # 学生ID
    "embedding": vector(1536),    # 特征向量
    "research_interests": string, # 研究兴趣
    "degree_level": string,       # 学位级别
    "gpa": float,                 # GPA
    "major": string,              # 专业
    "graduation_year": int,       # 毕业年份
    "update_time": timestamp      # 更新时间
}
```

#### 2.2.2 特征向量生成

**导师特征文本构建：**
```
导师：{name}
职称：{title}
机构：{institution}
研究方向：{researchAreas}
个人简介：{bio}
教育背景：{educationBackground}
关键词：{keywords}
```

**学生特征文本构建：**
```
学生：{name}
学历：{degreeLevel}
专业：{major}
学校：{currentInstitution}
GPA：{gpa}
研究兴趣：{researchInterests}
个人简介：{bio}
关键词：{keywords}
```

使用LLM Embedding API将文本转换为向量。

#### 2.2.3 多维度评分系统

**学生找导师的评分维度：**

1. **研究方向匹配度** (权重: 40%)
   - 向量相似度得分
   - 关键词重叠度

2. **导师质量** (权重: 25%)
   - 平均评分
   - 评价数量
   - 是否认证

3. **接收能力** (权重: 20%)
   - 是否接收学生
   - 剩余名额比例 = (max_students - current_students) / max_students

4. **工作强度评估** (权重: 15%)
   - 通过评价中的关键词分析（"压力大"、"工作量"等）
   - 学生反馈的平均工作时长

**导师找学生的评分维度：**

1. **研究兴趣匹配度** (权重: 35%)
   - 向量相似度得分
   - 关键词重叠度

2. **学术能力** (权重: 30%)
   - GPA分数
   - 学历层次（PhD > Master > Bachelor）
   - 学校排名（如果有）

3. **背景匹配** (权重: 20%)
   - 专业相关性
   - 研究经历

4. **时间匹配** (权重: 15%)
   - 毕业年份与招生时间的匹配

### 2.3 推荐流程

#### 2.3.1 学生获取导师推荐流程

```
1. 获取学生资料（MySQL）
2. 检查是否已有特征向量（Milvus）
   - 如果没有或已过期，生成新向量并存储
3. 在Milvus中搜索相似导师（Top 50）
4. 对每个候选导师计算多维度评分
5. 使用LLM分析Top 20导师，生成推荐理由
6. 按综合得分排序，返回Top 10
```

#### 2.3.2 导师获取学生推荐流程

```
1. 获取导师资料（MySQL）
2. 检查是否已有特征向量（Milvus）
   - 如果没有或已过期，生成新向量并存储
3. 在Milvus中搜索相似学生（Top 50）
4. 对每个候选学生计算多维度评分
5. 使用LLM分析Top 20学生，生成推荐理由
6. 按综合得分排序，返回Top 10
```

## 3. 实现计划

### 3.1 依赖添加

**pom.xml:**
```xml
<!-- Milvus Java SDK -->
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.3.4</version>
</dependency>
```

### 3.2 配置文件

**application.yml:**
```yaml
milvus:
  host: 127.0.0.1
  port: 19530
  database: mentor_system
  collections:
    mentor-profiles: mentor_profiles
    student-profiles: student_profiles
  embedding:
    dimension: 1536
    metric-type: COSINE
```

### 3.3 核心类设计

#### 3.3.1 MilvusService.java
- 连接Milvus
- 创建/管理Collection
- 插入/更新向量
- 向量相似度搜索

#### 3.3.2 EmbeddingService.java
- 调用LLM Embedding API
- 生成导师/学生特征向量
- 缓存向量（Redis）

#### 3.3.3 EnhancedRecommendationService.java
- 协调整个推荐流程
- 多维度评分计算
- LLM推荐理由生成

#### 3.3.4 RecommendationScorer.java
- 实现各维度评分算法
- 权重配置管理
- 综合得分计算

### 3.4 数据库表设计

**新增表：vector_sync_status**
```sql
CREATE TABLE vector_sync_status (
    id INT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(20) NOT NULL,  -- 'mentor' or 'student'
    entity_id INT NOT NULL,
    vector_id VARCHAR(100),             -- Milvus中的向量ID
    last_sync_time DATETIME,
    content_hash VARCHAR(64),           -- 内容哈希，用于检测变化
    status VARCHAR(20),                 -- 'synced', 'pending', 'failed'
    UNIQUE KEY uk_entity (entity_type, entity_id)
);
```

## 4. API设计

### 4.1 学生获取导师推荐

**请求：**
```
GET /api/recommendations/mentors?userId={userId}&limit={limit}
```

**响应：**
```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "mentor": {
        "id": 1,
        "name": "张教授",
        "title": "教授",
        "institution": "清华大学",
        "researchAreas": ["机器学习", "计算机视觉"],
        "ratingAvg": 4.8,
        "acceptingStudents": true
      },
      "score": 0.92,
      "matchDetails": {
        "researchMatch": 0.95,
        "qualityScore": 0.90,
        "availabilityScore": 0.85,
        "workloadScore": 0.88
      },
      "reason": "该导师的研究方向与您的兴趣高度匹配，特别是在深度学习和计算机视觉领域。导师评价优秀，目前还有招生名额，工作强度适中。"
    }
  ]
}
```

### 4.2 导师获取学生推荐

**请求：**
```
GET /api/recommendations/students?userId={userId}&limit={limit}
```

**响应：**
```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "student": {
        "id": 1,
        "name": "李同学",
        "degreeLevel": "Master",
        "major": "计算机科学",
        "currentInstitution": "北京大学",
        "gpa": 3.8,
        "researchInterests": ["深度学习", "自然语言处理"]
      },
      "score": 0.89,
      "matchDetails": {
        "researchMatch": 0.92,
        "academicAbility": 0.88,
        "backgroundMatch": 0.85,
        "timeMatch": 0.90
      },
      "reason": "该学生的研究兴趣与您的方向高度契合，学术成绩优秀，具有扎实的计算机基础，预计明年毕业，时间安排合适。"
    }
  ]
}
```

## 5. 性能优化

### 5.1 缓存策略

1. **向量缓存**：使用Redis缓存生成的向量，避免重复调用Embedding API
2. **推荐结果缓存**：缓存推荐结果1小时，减少计算开销
3. **用户画像缓存**：缓存用户的特征文本和向量

### 5.2 异步处理

1. **向量同步**：使用异步任务定期同步MySQL数据到Milvus
2. **批量更新**：批量生成向量和更新Milvus

### 5.3 索引优化

1. **Milvus索引**：使用IVF_FLAT或HNSW索引加速搜索
2. **MySQL索引**：为常用查询字段添加索引

## 6. 监控和日志

### 6.1 关键指标

1. **推荐质量**：
   - 推荐点击率
   - 推荐转化率（申请/联系）
   - 用户反馈评分

2. **系统性能**：
   - 推荐响应时间
   - Milvus查询延迟
   - LLM API调用延迟
   - 缓存命中率

3. **数据同步**：
   - 向量同步成功率
   - 数据一致性检查

### 6.2 日志记录

记录以下关键事件：
- 推荐请求和响应
- 向量生成和同步
- 评分计算详情
- 异常和错误

## 7. 测试计划

### 7.1 单元测试

- MilvusService连接和操作测试
- EmbeddingService向量生成测试
- RecommendationScorer评分算法测试

### 7.2 集成测试

- 完整推荐流程测试
- 多维度评分综合测试
- 缓存和性能测试

### 7.3 用户测试

- A/B测试对比新旧推荐系统
- 收集用户反馈
- 调整权重和算法

## 8. 部署和运维

### 8.1 Milvus部署

使用Docker部署Milvus：
```bash
docker run -d --name milvus-standalone \
  -p 19530:19530 \
  -p 9091:9091 \
  -v /home/sun/milvus/volumes/milvus:/var/lib/milvus \
  milvusdb/milvus:latest
```

### 8.2 数据迁移

1. 创建Milvus Collections
2. 批量生成现有导师和学生的向量
3. 同步到Milvus
4. 验证数据完整性

### 8.3 监控告警

- Milvus服务健康检查
- 向量同步失败告警
- API调用失败告警
- 性能指标异常告警

## 9. 未来扩展

1. **个性化权重**：根据用户历史行为动态调整评分权重
2. **协同过滤**：结合相似用户的选择进行推荐
3. **时间序列分析**：考虑用户兴趣的时间变化
4. **多模态推荐**：结合论文、项目等多种数据源
5. **实时推荐**：基于用户实时行为动态更新推荐
