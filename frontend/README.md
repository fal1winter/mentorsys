# 导师推荐系统 - 前端

基于 Vue 3 + Ant Design Vue 的导师推荐系统前端应用。

## 技术栈

- **Vue 3.3.4** - 渐进式 JavaScript 框架
- **Vue Router 4.2.4** - 官方路由管理器
- **Vuex 4.1.0** - 状态管理
- **Ant Design Vue 3.2.20** - UI 组件库
- **Axios 1.4.0** - HTTP 客户端
- **SockJS + STOMP** - WebSocket 实时通信
- **Day.js** - 日期处理

## 项目结构

```
frontend/
├── public/
│   └── index.html          # HTML 模板
├── src/
│   ├── components/         # 组件目录
│   │   ├── auth/          # 认证组件
│   │   │   ├── Login.vue
│   │   │   └── Register.vue
│   │   ├── mentor/        # 导师相关组件
│   │   │   ├── MentorList.vue
│   │   │   ├── MentorCard.vue
│   │   │   └── MentorDetail.vue
│   │   ├── student/       # 学生相关组件
│   │   │   ├── StudentDashboard.vue
│   │   │   └── StudentProfile.vue
│   │   ├── application/   # 申请相关组件
│   │   │   └── ApplicationList.vue
│   │   ├── chat/          # 聊天组件
│   │   │   └── ChatRoom.vue
│   │   ├── recommendation/# 推荐组件
│   │   │   └── RecommendedMentors.vue
│   │   └── admin/         # 管理员组件
│   │       └── AdminDashboard.vue
│   ├── router/            # 路由配置
│   │   └── index.js
│   ├── store/             # Vuex 状态管理
│   │   ├── index.js
│   │   └── modules/
│   │       ├── auth.js
│   │       ├── mentor.js
│   │       ├── student.js
│   │       ├── application.js
│   │       └── chat.js
│   ├── service/           # API 服务层
│   │   ├── axios.js
│   │   ├── authService.js
│   │   ├── mentorService.js
│   │   ├── studentService.js
│   │   ├── applicationService.js
│   │   ├── chatService.js
│   │   ├── recommendationService.js
│   │   └── ratingService.js
│   ├── App.vue            # 根组件
│   └── main.js            # 入口文件
├── package.json           # 依赖配置
└── vue.config.js          # Vue CLI 配置

```

## 快速开始

### 1. 安装依赖

```bash
cd /home/sun/mentor-system/frontend
npm install
```

### 2. 开发模式

```bash
npm run serve
```

应用将在 `http://localhost:7021` 启动，并自动代理后端 API 请求到 `http://localhost:7020`。

### 3. 生产构建

```bash
npm run build
```

构建产物将生成在 `dist/` 目录。

## 核心功能

### 1. 用户认证
- 登录/注册
- 角色管理（学生、导师、管理员）
- 会话保持

### 2. 导师浏览
- 导师列表展示
- 搜索和筛选
- 导师详情页
- 学术成果展示
- 评分评价查看

### 3. 个性化推荐
- 基于浏览历史的 LLM 偏好分析
- 智能导师推荐
- 推荐理由展示

### 4. 申请工作流
- 学生申请导师
- 导师接受/拒绝申请
- 申请状态跟踪
- 申请列表管理

### 5. 实时聊天
- WebSocket 实时通信
- 消息持久化
- 已读状态
- 聊天历史

### 6. 学生中心
- 个人资料管理
- 申请历史
- 导师评价

### 7. 管理员控制台
- 系统统计
- 用户管理
- 申请监控

## API 代理配置

`vue.config.js` 中配置了开发环境的 API 代理：

```javascript
devServer: {
  port: 7021,
  proxy: {
    '/api': {
      target: 'http://localhost:7020',
      changeOrigin: true
    },
    '/ws': {
      target: 'http://localhost:7020',
      changeOrigin: true,
      ws: true
    }
  }
}
```

## 路由说明

| 路径 | 组件 | 说明 | 权限 |
|------|------|------|------|
| `/` | - | 重定向到 `/mentors` | 公开 |
| `/login` | Login | 登录页 | 游客 |
| `/register` | Register | 注册页 | 游客 |
| `/mentors` | MentorList | 导师列表 | 公开 |
| `/mentors/:id` | MentorDetail | 导师详情 | 公开 |
| `/recommendations` | RecommendedMentors | 个性化推荐 | 学生 |
| `/student/profile` | StudentProfile | 学生资料 | 学生 |
| `/student/dashboard` | StudentDashboard | 学生中心 | 学生 |
| `/applications` | ApplicationList | 申请列表 | 认证用户 |
| `/chat/:applicationId` | ChatRoom | 聊天室 | 认证用户 |
| `/admin` | AdminDashboard | 管理员控制台 | 管理员 |

## 状态管理

### Auth Module
- 用户登录状态
- 用户信息
- Token 管理

### Mentor Module
- 导师列表
- 当前导师详情
- 加载状态

### Student Module
- 学生资料
- 资料更新

### Application Module
- 申请列表
- 申请操作

### Chat Module
- 聊天消息
- 未读消息统计
- WebSocket 连接状态

## 开发注意事项

1. **认证拦截器**：所有 API 请求自动携带 Token
2. **路由守卫**：根据用户角色控制页面访问权限
3. **WebSocket 连接**：进入聊天室自动建立连接，离开时断开
4. **错误处理**：统一的错误提示和处理
5. **响应式设计**：适配不同屏幕尺寸

## 部署说明

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name mentor.papervote.top;

    # 前端静态文件
    location / {
        root /home/sun/mentor-system/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API
    location /api {
        proxy_pass http://127.0.0.1:7020/api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:7020/ws;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
    }
}
```

## 常见问题

### 1. 依赖安装失败
```bash
# 清除缓存重试
rm -rf node_modules package-lock.json
npm install
```

### 2. WebSocket 连接失败
- 检查后端 WebSocket 服务是否启动
- 确认代理配置正确
- 查看浏览器控制台错误信息

### 3. API 请求 401 错误
- 检查 Token 是否过期
- 重新登录获取新 Token

## 后续优化

- [ ] 添加单元测试
- [ ] 优化移动端体验
- [ ] 添加国际化支持
- [ ] 性能优化（懒加载、代码分割）
- [ ] 添加 PWA 支持
