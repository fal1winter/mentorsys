# 导师推荐系统 - Mentor Recommendation System

## 项目状态：前后端完整实现 ✅✅

**后端**: Spring Boot 2.4.3 - 8个完整系统 ✅
**前端**: Vue 3.3.4 + Ant Design Vue - 完整UI实现 ✅
**数据库**: MySQL + MongoDB + Redis - 13个表 ✅
**推荐系统**: 增强版多维度推荐 + 向量语义搜索 ✅

## 核心功能（8个完整系统）

### 1. 认证系统 ✅
- 用户注册、登录、登出
- Apache Shiro + BCrypt加密
- 角色权限管理（ADMIN、MENTOR、STUDENT）
- **Redis Session持久化**：后端重启不丢失登录状态
- API: `/api/auth/*`

### 2. 导师管理 ✅
- 完整CRUD操作
- 搜索、筛选（机构、研究方向）
- 浏览次数统计
- API: `/api/mentors/*`

### 3. 学生管理 ✅
- 完整CRUD操作
- 个人资料管理
- 搜索功能
- API: `/api/students/*`

### 4. 论文管理 ✅
- 完整CRUD操作
- 关联导师
- 按年份、关键词搜索
- API: `/api/publications/*`

### 5. 申请工作流 ✅
- 学生申请导师
- 导师接受/拒绝申请
- 申请状态跟踪（pending/accepted/rejected/withdrawn）
- 自动更新导师学生数
- API: `/api/applications/*`

### 6. 评分系统 ✅
- 学生评价导师（1-5星）
- 多维度评分（指导、沟通、资源）
- 自动更新导师平均分
- 有用标记功能
- API: `/api/ratings/*`

### 7. 增强版推荐引擎 ✅✅
- **双向推荐**：学生找导师 + 导师找学生
- **多维度评分**：
  - 学生找导师：研究方向匹配(40%) + 导师质量(25%) + 接收能力(20%) + 工作强度(15%)
  - 导师找学生：研究兴趣匹配(35%) + 学术能力(30%) + 背景匹配(20%) + 时间匹配(15%)
- **向量语义搜索**：基于Embedding的相似度匹配
- **LLM智能分析**：DeepSeek生成个性化推荐理由
- **Redis缓存**：推荐结果缓存优化性能
- API: `/api/recommendations/*`

### 8. WebSocket聊天 ✅
- 实时通信（STOMP协议）
- 消息持久化
- 未读消息统计
- 已读状态跟踪
- **直接聊天**：支持学生导师直接对话
- WebSocket: `/ws`, API: `/api/chat/*`

## 技术架构

### 后端技术栈
- **框架**: Spring Boot 2.4.3
- **ORM**: MyBatis 3.5.15
- **数据库**: MySQL 8.0 + MongoDB + Redis
- **认证**: Apache Shiro 1.7.1
- **实时通信**: WebSocket + STOMP
- **AI集成**: OpenRouter API + DeepSeek
- **向量搜索**: Python语义搜索服务
- **构建工具**: Maven

### 前端技术栈
- **框架**: Vue 3.3.4
- **路由**: Vue Router 4.2.4
- **状态管理**: Vuex 4.1.0
- **UI组件**: Ant Design Vue 3.2.20
- **HTTP客户端**: Axios 1.4.0
- **WebSocket**: SockJS + STOMP
- **构建工具**: Vue CLI 5.0.8

### 数据库设计
**数据库名**: `mentor_system`

**13个核心表**:
- `users` - 用户认证
- `mentors` - 导师信息
- `students` - 学生信息
- `publications` - 学者作品/论文
- `applications` - 申请匹配
- `ratings` - 评分评价
- `browsing_history` - 浏览历史
- `user_preferences` - 用户偏好（LLM分析）
- `chat_messages` - 聊天消息
- `roles`, `permissions`, `user_roles`, `role_permissions` - RBAC权限

## 项目文件统计

### 后端文件
- **Controllers**: 8个
- **Services**: 10个
- **Mappers**: 8个（Java接口 + XML映射）
- **Entities**: 13个
- **配置文件**: application.yml, pom.xml, schema.sql, WebSocketConfig, ShiroConfig

### 前端文件
- **Vue组件**: 12个（认证、导师、学生、申请、聊天、推荐、管理员）
- **Vuex模块**: 5个（auth, mentor, student, application, chat）
- **服务层**: 7个（axios, authService, mentorService, studentService, applicationService, chatService, recommendationService, ratingService）
- **路由配置**: 11个路由
- **配置文件**: package.json, vue.config.js

## 快速启动

### 1. 数据库准备
```bash
# 数据库已创建并初始化
mysql -u root -p22222222 mentor_system
```

### 2. 启动后端
```bash
cd /home/sun/mentor-system/backend
mvn clean package
java -jar target/mentor-system.jar
```

后端将在 `http://localhost:7020` 启动

### 3. 启动前端
```bash
cd /home/sun/mentor-system/frontend
npm install
npm run serve
```

前端将在 `http://localhost:7021` 启动

### 4. 访问应用
- **前端界面**: http://localhost:7021
- **后端API**: http://localhost:7020/api
- **API文档**: http://localhost:7020/api/doc.html
- **WebSocket**: ws://localhost:7020/ws

## API端点总览

| 模块 | 端点 | 说明 |
|------|------|------|
| 认证 | `/api/auth/*` | 注册、登录、权限管理 |
| 导师 | `/api/mentors/*` | 导师CRUD、搜索、筛选 |
| 学生 | `/api/students/*` | 学生CRUD、搜索 |
| 论文 | `/api/publications/*` | 论文CRUD、搜索 |
| 申请 | `/api/applications/*` | 申请工作流 |
| 评分 | `/api/ratings/*` | 评分评价系统 |
| 推荐 | `/api/recommendations/*` | 个性化推荐 |
| 聊天 | `/api/chat/*`, `/ws` | 实时聊天 |

## 配置说明

### application.yml 关键配置
- **服务端口**: 7020
- **上下文路径**: /api
- **MySQL**: localhost:3306/mentor_system
- **MongoDB**: localhost:27017/mentor_system
- **Redis**: localhost:6379
- **Session超时**: 30分钟（存储在Redis）
- **LLM API**: OpenRouter + DeepSeek
- **WebSocket**: /ws

### Redis Session配置
系统使用Redis存储Shiro Session，解决后端重启后用户掉线问题：
- **Session前缀**: `mentor:session:`
- **序列化方式**: JDK序列化（支持Shiro Session复杂对象）
- **超时时间**: 30分钟（可在application.yml中配置）
- **自动续期**: 每次访问自动刷新过期时间

## 前端功能清单 ✅

### 已实现的页面和功能
1. **用户认证** ✅
   - 登录页面（Login.vue）
   - 注册页面（Register.vue）
   - 角色选择（学生/导师）

2. **导师浏览** ✅
   - 导师列表（MentorList.vue）
   - 导师卡片（MentorCard.vue）
   - 导师详情（MentorDetail.vue）
   - 搜索和筛选功能
   - 学术成果展示
   - 评分评价查看

3. **学生中心** ✅
   - 学生仪表板（StudentDashboard.vue）
   - 个人资料管理（StudentProfile.vue）
   - 申请历史查看

4. **申请管理** ✅
   - 申请列表（ApplicationList.vue）
   - 申请/接受/拒绝/撤回
   - 申请状态跟踪

5. **实时聊天** ✅
   - 聊天室（ChatRoom.vue）
   - WebSocket实时通信
   - 消息历史

6. **个性化推荐** ✅
   - 推荐页面（RecommendedMentors.vue）
   - LLM偏好分析展示
   - 推荐理由说明

7. **管理员控制台** ✅
   - 管理员仪表板（AdminDashboard.vue）
   - 系统统计
   - 申请监控

## 部署配置

### Nginx配置示例
创建文件 `/etc/nginx/sites-available/mentor.papervote.top`：

```nginx
server {
    listen 80;
    server_name mentor.papervote.top;

    # 前端静态文件
    location / {
        root /home/sun/mentor-system/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端API
    location /api {
        proxy_pass http://127.0.0.1:7020/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:7020/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
        proxy_set_header Host $host;
    }
}
```

### 部署步骤
```bash
# 1. 构建前端
cd /home/sun/mentor-system/frontend
npm run build

# 2. 构建后端
cd /home/sun/mentor-system/backend
mvn clean package

# 3. 配置Nginx
sudo ln -s /etc/nginx/sites-available/mentor.papervote.top /etc/nginx/sites-enabled/
sudo nginx -t
sudo nginx -s reload

# 4. 启动后端服务
java -jar /home/sun/mentor-system/backend/target/mentor-system.jar
```

## 项目特色

1. **智能推荐** - 基于LLM的用户偏好分析和个性化推荐
2. **实时通信** - WebSocket实现学生导师即时沟通
3. **完整工作流** - 从浏览、申请到评价的闭环流程
4. **权限管理** - 基于Shiro的细粒度权限控制

## 项目完成度

| 模块 | 状态 | 说明 |
|------|------|------|
| 数据库设计 | ✅ 100% | 13个表，完整schema |
| 后端API | ✅ 100% | 8个完整系统，58MB JAR |
| 前端UI | ✅ 100% | 12个Vue组件，完整交互 |
| WebSocket | ✅ 100% | 实时聊天功能 |
| LLM推荐 | ✅ 100% | DeepSeek偏好分析 |
| 认证授权 | ✅ 100% | Shiro + RBAC |
| 文档 | ✅ 100% | 前后端README |

---

**当前状态**: 前后端核心功能100%完成，可直接部署上线 ✅✅

**项目地址**: `/home/sun/mentor-system/`
**域名**: mentor.papervote.top（待配置）
**开发时间**: 2026-02-01
