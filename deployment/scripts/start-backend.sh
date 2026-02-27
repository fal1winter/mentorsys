#!/bin/bash

# Mentor System Backend Startup Script
# 导师推荐系统后端启动脚本

echo "=========================================="
echo "Starting Mentor Recommendation System Backend"
echo "=========================================="

# Navigate to backend directory
cd /home/sun/mentor-system/backend

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Error: Java is not installed"
    exit 1
fi

echo "Building project with Maven..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Error: Maven build failed"
    exit 1
fi

echo "Starting backend service on port 7020..."
java -jar target/mentor-system.jar

echo "Backend service stopped"
