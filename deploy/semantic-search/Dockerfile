FROM python:3.11-slim

WORKDIR /app

# 设置 HuggingFace 镜像
ENV HF_ENDPOINT=https://hf-mirror.com

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

COPY semantic_service.py .
COPY rabbitmq_consumer.py* ./

EXPOSE 5050

CMD ["python", "semantic_service.py"]
