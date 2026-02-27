<template>
  <a-layout style="min-height: 100vh">
    <!-- 侧边栏 -->
    <a-layout-sider
      v-model:collapsed="collapsed"
      :trigger="null"
      collapsible
      :width="240"
      style="background: #001529"
    >
      <div class="logo">
        <h2 v-if="!collapsed">导师推荐系统</h2>
        <h2 v-else>导师</h2>
      </div>

      <!-- 导航菜单 -->
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        @click="handleMenuClick"
      >
        <!-- 公共菜单 -->
        <a-menu-item key="/mentors">
          <TeamOutlined />
          <span>导师列表</span>
        </a-menu-item>

        <a-menu-item key="/students">
          <UserOutlined />
          <span>学生列表</span>
        </a-menu-item>

        <!-- 学生菜单 -->
        <template v-if="userRole === 'STUDENT'">
          <a-menu-item key="/recommendations">
            <StarOutlined />
            <span>推荐导师</span>
          </a-menu-item>
          <a-menu-item key="/applications">
            <FileTextOutlined />
            <span>我的申请</span>
          </a-menu-item>
          <a-menu-item key="/student/dashboard">
            <DashboardOutlined />
            <span>学生面板</span>
          </a-menu-item>
        </template>

        <!-- 导师菜单 -->
        <template v-if="userRole === 'MENTOR'">
          <a-menu-item key="/recommendations/students">
            <StarOutlined />
            <span>推荐学生</span>
          </a-menu-item>
          <a-menu-item key="/mentor/students">
            <TeamOutlined />
            <span>我的学生</span>
          </a-menu-item>
          <a-menu-item key="/applications">
            <FileTextOutlined />
            <span>申请管理</span>
          </a-menu-item>
        </template>

        <!-- 聊天入口（学生和导师都可见） -->
        <a-menu-item key="/chats" v-if="isAuthenticated && (userRole === 'STUDENT' || userRole === 'MENTOR')">
          <a-badge :count="unreadCount" :offset="[10, 0]" size="small">
            <MessageOutlined />
          </a-badge>
          <span style="margin-left: 10px">我的聊天</span>
        </a-menu-item>

        <!-- 个人资料 -->
        <a-menu-item key="/profile" v-if="isAuthenticated">
          <UserOutlined />
          <span>个人资料</span>
        </a-menu-item>

        <!-- 管理员菜单 -->
        <a-sub-menu key="admin" v-if="userRole === 'ADMIN'">
          <template #icon>
            <SettingOutlined />
          </template>
          <template #title>系统管理</template>
          <a-menu-item key="/admin">
            <DashboardOutlined />
            <span>管理面板</span>
          </a-menu-item>
          <a-menu-item key="/admin/users">
            <UserOutlined />
            <span>用户管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/mentors">
            <TeamOutlined />
            <span>导师管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/keywords">
            <TagsOutlined />
            <span>关键词管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/publications">
            <FileTextOutlined />
            <span>论文管理</span>
          </a-menu-item>
          <a-menu-item key="/admin/ratings">
            <StarOutlined />
            <span>评分管理</span>
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <!-- 主内容区 -->
    <a-layout>
      <!-- 顶部导航栏 -->
      <a-layout-header style="background: #fff; padding: 0; display: flex; justify-content: space-between; align-items: center; box-shadow: 0 1px 4px rgba(0,21,41,.08)">
        <div style="padding-left: 24px">
          <MenuUnfoldOutlined
            v-if="collapsed"
            class="trigger"
            @click="collapsed = !collapsed"
          />
          <MenuFoldOutlined
            v-else
            class="trigger"
            @click="collapsed = !collapsed"
          />
        </div>

        <div style="padding-right: 24px">
          <a-space v-if="isAuthenticated">
            <a-badge :count="unreadCount" :offset="[-5, 5]">
              <a-button type="text" @click="$router.push('/chats')" v-if="userRole === 'STUDENT' || userRole === 'MENTOR'">
                <MessageOutlined />
              </a-button>
            </a-badge>
            <span>{{ username }}</span>
            <a-tag :color="getRoleColor(userRole)">{{ getRoleText(userRole) }}</a-tag>
            <a-button type="link" @click="handleLogout">
              <LogoutOutlined />
              退出登录
            </a-button>
          </a-space>
          <a-space v-else>
            <a-button type="link" @click="$router.push('/login')">登录</a-button>
            <a-button type="primary" @click="$router.push('/register')">注册</a-button>
          </a-space>
        </div>
      </a-layout-header>

      <!-- 内容区域 -->
      <a-layout-content style="margin: 24px 16px; padding: 24px; background: #fff; min-height: 280px">
        <router-view />
      </a-layout-content>

      <!-- 底部 -->
      <a-layout-footer style="text-align: center">
        导师推荐系统 ©2026
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script>
import { defineComponent, ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { message } from 'ant-design-vue'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  TeamOutlined,
  StarOutlined,
  FileTextOutlined,
  DashboardOutlined,
  SettingOutlined,
  TagsOutlined,
  LogoutOutlined,
  MessageOutlined
} from '@ant-design/icons-vue'
import chatService from '../../service/chatService'

export default defineComponent({
  name: 'MainLayout',
  components: {
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    UserOutlined,
    TeamOutlined,
    StarOutlined,
    FileTextOutlined,
    DashboardOutlined,
    SettingOutlined,
    TagsOutlined,
    LogoutOutlined,
    MessageOutlined
  },
  setup() {
    const router = useRouter()
    const route = useRoute()
    const store = useStore()

    const collapsed = ref(false)
    const selectedKeys = ref([route.path])
    const unreadCount = ref(0)
    let unreadTimer = null

    const isAuthenticated = computed(() => store.getters['auth/isAuthenticated'])
    const username = computed(() => store.getters['auth/username'])
    const userRole = computed(() => store.getters['auth/userRole'])
    const currentUser = computed(() => store.getters['auth/currentUser'])

    // 监听路由变化，更新选中的菜单项
    watch(() => route.path, (newPath) => {
      selectedKeys.value = [newPath]
    })

    // 获取未读消息数
    const fetchUnreadCount = async () => {
      if (!isAuthenticated.value || !currentUser.value?.id) return
      if (userRole.value !== 'STUDENT' && userRole.value !== 'MENTOR') return
      
      try {
        const response = await chatService.getTotalUnreadCount(currentUser.value.id, userRole.value)
        if (response.code === 0) {
          unreadCount.value = response.count || 0
        }
      } catch (error) {
        console.error('获取未读消息数失败:', error)
      }
    }

    const handleMenuClick = ({ key }) => {
      router.push(key)
    }

    const handleLogout = async () => {
      try {
        await store.dispatch('auth/logout')
        message.success('退出登录成功')
        router.push('/login')
      } catch (error) {
        message.error('退出登录失败')
      }
    }

    const getRoleText = (role) => {
      const roleMap = {
        'ADMIN': '管理员',
        'MENTOR': '导师',
        'STUDENT': '学生'
      }
      return roleMap[role] || role
    }

    const getRoleColor = (role) => {
      const colorMap = {
        'ADMIN': 'red',
        'MENTOR': 'blue',
        'STUDENT': 'green'
      }
      return colorMap[role] || 'default'
    }

    onMounted(() => {
      fetchUnreadCount()
      // 每30秒刷新一次未读消息数
      unreadTimer = setInterval(fetchUnreadCount, 30000)
    })

    onUnmounted(() => {
      if (unreadTimer) {
        clearInterval(unreadTimer)
      }
    })

    return {
      collapsed,
      selectedKeys,
      isAuthenticated,
      username,
      userRole,
      unreadCount,
      handleMenuClick,
      handleLogout,
      getRoleText,
      getRoleColor
    }
  }
})
</script>

<style scoped>
.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo h2 {
  color: #fff;
  margin: 0;
  font-size: 18px;
}

.trigger {
  font-size: 18px;
  cursor: pointer;
  transition: color 0.3s;
}

.trigger:hover {
  color: #1890ff;
}
</style>
