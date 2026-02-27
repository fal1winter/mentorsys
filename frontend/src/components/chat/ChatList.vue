<template>
  <div class="chat-list-container">
    <a-page-header title="我的聊天" sub-title="与导师/学生的对话列表">
      <template #extra>
        <a-button @click="loadConversations" :loading="loading">
          <ReloadOutlined /> 刷新
        </a-button>
      </template>
    </a-page-header>

    <a-spin :spinning="loading">
      <div v-if="conversations.length > 0" class="conversation-list">
        <a-card v-for="conv in conversations" :key="conv.id" class="conversation-item" hoverable @click="openChat(conv)">
          <div class="conversation-content">
            <a-avatar :size="48" :src="conv.partnerAvatar">{{ conv.partnerName?.charAt(0) }}</a-avatar>
            <div class="conversation-info">
              <div class="conversation-header">
                <span class="partner-name">{{ conv.partnerName }}</span>
                <a-tag :color="conv.partnerType === 'MENTOR' ? 'blue' : 'green'" size="small">
                  {{ conv.partnerType === 'MENTOR' ? '导师' : '学生' }}
                </a-tag>
                <span class="last-time">{{ formatTime(conv.lastMessageTime) }}</span>
              </div>
              <div class="last-message">
                <span v-if="conv.lastMessage">{{ conv.lastMessage }}</span>
                <span v-else class="no-message">暂无消息</span>
              </div>
            </div>
            <a-badge v-if="conv.unreadCount > 0" :count="conv.unreadCount" class="unread-badge" />
          </div>
        </a-card>
      </div>
      <a-empty v-else description="暂无聊天记录">
        <template #extra>
          <p style="color: #999">在推荐页面点击"发起聊天"开始对话</p>
        </template>
      </a-empty>
    </a-spin>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { ReloadOutlined } from '@ant-design/icons-vue'
import chatService from '../../service/chatService'
import dayjs from 'dayjs'
import isToday from 'dayjs/plugin/isToday'
import isYesterday from 'dayjs/plugin/isYesterday'
import 'dayjs/locale/zh-cn'

dayjs.extend(isToday)
dayjs.extend(isYesterday)
dayjs.locale('zh-cn')

export default defineComponent({
  name: 'ChatList',
  components: { ReloadOutlined },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const conversations = ref([])

    const currentUser = computed(() => store.getters['auth/currentUser'])
    const userRole = computed(() => store.getters['auth/userRole'])

    const loadConversations = async () => {
      if (!currentUser.value?.id) return
      loading.value = true
      try {
        const response = await chatService.getConversations(currentUser.value.id, userRole.value)
        if (response.code === 0) {
          conversations.value = response.data || []
        }
      } catch (error) {
        console.error('加载聊天列表失败:', error)
      } finally {
        loading.value = false
      }
    }

    const openChat = (conv) => {
      router.push(`/chat/direct/${conv.studentId}/${conv.mentorId}`)
    }

    const formatTime = (time) => {
      if (!time) return ''
      const d = dayjs(time)
      if (d.isToday()) return d.format('HH:mm')
      if (d.isYesterday()) return '昨天'
      return d.format('MM-DD')
    }

    onMounted(() => { loadConversations() })

    return { loading, conversations, loadConversations, openChat, formatTime }
  }
})
</script>

<style scoped>
.chat-list-container { max-width: 800px; margin: 0 auto; padding: 24px; }
.conversation-list { display: flex; flex-direction: column; gap: 12px; }
.conversation-item { cursor: pointer; transition: all 0.3s; }
.conversation-item:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); transform: translateY(-2px); }
.conversation-content { display: flex; align-items: center; gap: 16px; }
.conversation-info { flex: 1; min-width: 0; }
.conversation-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.partner-name { font-weight: 500; font-size: 16px; }
.last-time { margin-left: auto; color: #999; font-size: 12px; }
.last-message { color: #666; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.no-message { color: #999; font-style: italic; }
.unread-badge { margin-left: 8px; }
</style>
