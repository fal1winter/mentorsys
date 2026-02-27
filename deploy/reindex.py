#!/usr/bin/env python3
"""
迁移后重新索引脚本
在新主机上 docker compose up 之后运行此脚本，将 MySQL 数据索引到 Milvus
"""
import json
import time
import requests
import pymysql

MYSQL_HOST = "127.0.0.1"
MYSQL_PORT = 3306
MYSQL_USER = "root"
MYSQL_PASSWORD = "22222222"
MYSQL_DB = "mentor_system"
SEMANTIC_URL = "http://127.0.0.1:5050"

def wait_for_service(url, name, retries=20):
    for i in range(retries):
        try:
            r = requests.get(f"{url}/health", timeout=5)
            if r.status_code == 200:
                print(f"{name} 已就绪")
                return True
        except:
            pass
        print(f"等待 {name}... ({i+1}/{retries})")
        time.sleep(5)
    raise RuntimeError(f"{name} 启动超时")

def reindex():
    wait_for_service(SEMANTIC_URL, "语义服务")

    conn = pymysql.connect(
        host=MYSQL_HOST, port=MYSQL_PORT,
        user=MYSQL_USER, password=MYSQL_PASSWORD,
        database=MYSQL_DB, charset='utf8mb4'
    )
    cursor = conn.cursor(pymysql.cursors.DictCursor)

    # 索引导师
    cursor.execute("SELECT id, name, research_areas, bio, institution, title FROM mentors")
    mentors = cursor.fetchall()
    print(f"索引导师: {len(mentors)} 条")
    for m in mentors:
        ra = m['research_areas']
        if isinstance(ra, str):
            try:
                ra = ' '.join(json.loads(ra))
            except:
                pass
        r = requests.post(f"{SEMANTIC_URL}/mentor/index", json={
            "id": m['id'],
            "name": m['name'] or '',
            "researchAreas": ra or '',
            "bio": m['bio'] or '',
            "institution": m['institution'] or '',
            "title": m['title'] or ''
        })
        print(f"  mentor {m['id']} {m['name']}: {r.json()}")

    # 索引学生
    cursor.execute("SELECT id, name, research_interests, bio, current_institution, major FROM students")
    students = cursor.fetchall()
    print(f"索引学生: {len(students)} 条")
    for s in students:
        ri = s['research_interests']
        if isinstance(ri, str):
            try:
                ri = ' '.join(json.loads(ri))
            except:
                pass
        r = requests.post(f"{SEMANTIC_URL}/student/index", json={
            "id": s['id'],
            "name": s['name'] or '',
            "researchInterests": ri or '',
            "bio": s['bio'] or '',
            "institution": s['current_institution'] or '',
            "major": s['major'] or ''
        })
        print(f"  student {s['id']} {s['name']}: {r.json()}")

    cursor.close()
    conn.close()

    stats = requests.get(f"{SEMANTIC_URL}/stats").json()
    print(f"\n索引完成: mentors={stats['mentors']['row_count']}, students={stats['students']['row_count']}")

if __name__ == '__main__':
    reindex()
