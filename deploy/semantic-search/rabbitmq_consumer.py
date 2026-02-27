#!/usr/bin/env python3
"""
RabbitMQ 消费者 - 消费论文和笔记的同步消息
"""

import pika
import json
import logging
import threading
import time
from typing import Callable

logger = logging.getLogger(__name__)

# RabbitMQ 配置
RABBITMQ_HOST = '127.0.0.1'
RABBITMQ_PORT = 5672
RABBITMQ_USER = 'javabackend'
RABBITMQ_PASSWORD = 'javabackend123'
RABBITMQ_VHOST = '/'

# 队列名称
MILVUS_SYNC_PAPER_QUEUE = 'milvus.sync.paper.queue'
MILVUS_SYNC_NOTE_QUEUE = 'milvus.sync.note.queue'
MILVUS_SYNC_SCHOLAR_QUEUE = 'milvus.sync.scholar.queue'


class RabbitMQConsumer:
    """RabbitMQ 消费者"""

    def __init__(self, milvus_client, model):
        self.milvus_client = milvus_client
        self.model = model
        self.connection = None
        self.channel = None
        self.is_running = False

    def connect(self):
        """连接到 RabbitMQ"""
        try:
            credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
            parameters = pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                port=RABBITMQ_PORT,
                virtual_host=RABBITMQ_VHOST,
                credentials=credentials,
                heartbeat=600,
                blocked_connection_timeout=300
            )
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            logger.info(f"Connected to RabbitMQ at {RABBITMQ_HOST}:{RABBITMQ_PORT}")
            return True
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {e}")
            return False

    def start_consuming(self):
        """开始消费消息"""
        if not self.connect():
            logger.error("Cannot start consuming - connection failed")
            return

        self.is_running = True

        # 声明队列（确保队列存在）
        self.channel.queue_declare(queue=MILVUS_SYNC_PAPER_QUEUE, durable=True)
        self.channel.queue_declare(queue=MILVUS_SYNC_NOTE_QUEUE, durable=True)
        self.channel.queue_declare(queue=MILVUS_SYNC_SCHOLAR_QUEUE, durable=True)

        # 设置 prefetch_count
        self.channel.basic_qos(prefetch_count=1)

        # 绑定消费者
        self.channel.basic_consume(
            queue=MILVUS_SYNC_PAPER_QUEUE,
            on_message_callback=self.on_paper_message,
            auto_ack=False
        )
        self.channel.basic_consume(
            queue=MILVUS_SYNC_NOTE_QUEUE,
            on_message_callback=self.on_note_message,
            auto_ack=False
        )
        self.channel.basic_consume(
            queue=MILVUS_SYNC_SCHOLAR_QUEUE,
            on_message_callback=self.on_scholar_message,
            auto_ack=False
        )

        logger.info("Started consuming messages from RabbitMQ")
        logger.info(f"Listening on queues: {MILVUS_SYNC_PAPER_QUEUE}, {MILVUS_SYNC_NOTE_QUEUE}, {MILVUS_SYNC_SCHOLAR_QUEUE}")

        try:
            self.channel.start_consuming()
        except KeyboardInterrupt:
            logger.info("Stopping consumer...")
            self.stop()
        except Exception as e:
            logger.error(f"Error in consumer: {e}")
            self.stop()

    def stop(self):
        """停止消费"""
        self.is_running = False
        if self.channel and self.channel.is_open:
            self.channel.stop_consuming()
        if self.connection and self.connection.is_open:
            self.connection.close()
        logger.info("RabbitMQ consumer stopped")

    def on_paper_message(self, ch, method, properties, body):
        """处理论文同步消息"""
        try:
            message = json.loads(body.decode('utf-8'))
            logger.info(f"Received paper sync message: paperId={message.get('paperId')}, op={message.get('operation')}")

            operation = message.get('operation')
            paper_id = message.get('paperId')

            if operation == 'DELETE':
                # 删除论文向量
                self.delete_paper_vector(paper_id)
            else:
                # 创建或更新论文向量
                self.index_paper(message)

            # 确认消息
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"Paper sync completed: paperId={paper_id}")

        except Exception as e:
            logger.error(f"Error processing paper message: {e}")
            # 拒绝消息并重新入队
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

    def on_note_message(self, ch, method, properties, body):
        """处理笔记同步消息"""
        try:
            message = json.loads(body.decode('utf-8'))
            logger.info(f"Received note sync message: noteId={message.get('noteId')}, op={message.get('operation')}")

            operation = message.get('operation')
            note_id = message.get('noteId')

            if operation == 'DELETE':
                # 删除笔记向量
                self.delete_note_vector(note_id)
            else:
                # 创建或更新笔记向量
                self.index_note(message)

            # 确认消息
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"Note sync completed: noteId={note_id}")

        except Exception as e:
            logger.error(f"Error processing note message: {e}")
            # 拒绝消息并重新入队
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

    def on_scholar_message(self, ch, method, properties, body):
        """处理学者同步消息（暂时只确认，不处理）"""
        try:
            message = json.loads(body.decode('utf-8'))
            logger.info(f"Received scholar sync message: scholarId={message.get('scholarId')}, op={message.get('operation')}")

            # TODO: 实现学者向量化
            logger.info("Scholar indexing not yet implemented, skipping")

            # 确认消息
            ch.basic_ack(delivery_tag=method.delivery_tag)

        except Exception as e:
            logger.error(f"Error processing scholar message: {e}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)

    def index_paper(self, message):
        """索引论文到 Milvus"""
        paper_id = message.get('paperId')
        title = message.get('title', '')
        abstract_text = message.get('abstractText', '')
        keywords = message.get('keywords', '')
        authors = message.get('authors', '')

        # 组合文本
        combined_text = f"{title} {abstract_text} {keywords} {authors}"

        # 生成向量
        vector = self.model.encode(combined_text, normalize_embeddings=True).tolist()

        # 准备元数据
        metadata = {
            "title": title[:500] if title else "",
            "abstract": abstract_text[:1000] if abstract_text else "",
            "keywords": keywords if keywords else ""
        }

        # 删除旧数据（如果存在）
        try:
            self.milvus_client.delete(collection_name="papers", ids=[paper_id])
        except:
            pass

        # 插入新数据
        self.milvus_client.insert(
            collection_name="papers",
            data=[{"id": paper_id, "vector": vector, **metadata}]
        )
        logger.info(f"Indexed paper: paperId={paper_id}")

    def delete_paper_vector(self, paper_id):
        """删除论文向量"""
        try:
            self.milvus_client.delete(collection_name="papers", ids=[paper_id])
            logger.info(f"Deleted paper vector: paperId={paper_id}")
        except Exception as e:
            logger.error(f"Error deleting paper vector: {e}")

    def index_note(self, message):
        """索引笔记到 Milvus"""
        note_id = message.get('noteId')
        paper_id = message.get('paperId')
        paper_title = message.get('paperTitle', '')
        summary = message.get('summary', '')
        note_content = message.get('noteContent', '')

        # 组合文本（限制长度）
        combined_text = f"{paper_title} {summary} {note_content[:2000] if note_content else ''}"

        # 生成向量
        vector = self.model.encode(combined_text, normalize_embeddings=True).tolist()

        # 准备元数据
        metadata = {
            "paper_id": paper_id,
            "paper_title": paper_title[:500] if paper_title else "",
            "summary": summary[:1000] if summary else ""
        }

        # 删除旧数据（如果存在）
        try:
            self.milvus_client.delete(collection_name="notes", ids=[note_id])
        except:
            pass

        # 插入新数据
        self.milvus_client.insert(
            collection_name="notes",
            data=[{"id": note_id, "vector": vector, **metadata}]
        )
        logger.info(f"Indexed note: noteId={note_id}")

    def delete_note_vector(self, note_id):
        """删除笔记向量"""
        try:
            self.milvus_client.delete(collection_name="notes", ids=[note_id])
            logger.info(f"Deleted note vector: noteId={note_id}")
        except Exception as e:
            logger.error(f"Error deleting note vector: {e}")


def start_consumer_thread(milvus_client, model):
    """在后台线程启动消费者"""
    consumer = RabbitMQConsumer(milvus_client, model)

    def run():
        while True:
            try:
                consumer.start_consuming()
            except Exception as e:
                logger.error(f"Consumer thread error: {e}")
                logger.info("Reconnecting in 5 seconds...")
                time.sleep(5)

    thread = threading.Thread(target=run, daemon=True)
    thread.start()
    logger.info("RabbitMQ consumer thread started")
    return consumer
