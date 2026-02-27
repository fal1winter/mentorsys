<template>
  <div class="direct-chat">
    <a-page-header
      title="直接聊天"
      :sub-title="chatPartnerName"
      @back="() => $router.back()"
    >
      <template #extra>
        <a-tag :color="connected ? 'green' : 'red'">
          {{ connected ? '已连接' : '未连接' }}
        </a-tag>
      </template>
    </a-page-header>

    <a-card class="chat-container">
      <div class="messages-container" ref="messagesContainer">
        <div v-if="messages.length === 0" class="empty-messages">
          <a-empty description="暂无消息，开始聊天吧" />
        </div>
        <div
          v-for="msg in messages"
          :key="msg.id || msg.timestamp"
          :class="['message', isSentByMe(msg) ? 'sent' : 'received']"
        >
          <div class="message-content">
            <div class="message-header">
              <span class="sender">{{ getSenderName(msg) }}</span>
              <span class="time">{{ formatTime(msg.createTime || msg.timestamp) }}</span>
            </div>
            <div class="message-text">{{ msg.content }}</div>
          </div>
        </div>
      </div>

      <div class="input-container">
        <a-textarea
          v-model:value="messageInput"
          :rows="3"
          placeholder="输入消息... (按Enter发送)"
          @pressEnter.prevent="handleSendMessage"
          :disabled="!connected"
        />
        <a-button 
          type="primary" 
          @click="handleSendMessage" 
          :loading="sending"
          :disabled="!connected || !messageInput.trim()"
        >
          <SendOutlined />
          发送
        </a-button>
      </div>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { SendOutlined } from '@ant-design/icons-vue'
import chatService from '../../service/chatService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'DirectChat',
  components: {
    SendOutlined
  },
  setup() {
    const store = useStore()
    const route = useRoute()
    const messageInput = ref('')
    const sending = ref(false)
    const messagesContainer = ref(null)
    const messages = ref([])
    const connected = ref(false)
    const chatPartnerName = ref('')
    let subscription = null

    const userRole = computed(() => store.getters['auth/userRole'])
    const currentUser = computed(() => store.getters['auth/currentUser'])
    
    const studentId = computed(() => parseInt(route.params.studentId))
    const mentorId = computed(() => parseInt(route.params.mentorId))
    
    // 生成聊天室ID
    const chatRoomId = computed(() => `direct_${studentId.value}_${mentorId.value}`)

    const isSentByMe = (msg) => {
      return msg.senderId === currentUser.value?.id
    }

    const getSenderName = (msg) => {
      if (isSentByMe(msg)) return '我'
      return msg.senderType === 'STUDENT' ? '学生' : '导师'
    }

    const connectWebSocket = () => {
      chatService.connect(
        () => {
          console.log('WebSocket connected')
          connected.value = true

          // 订阅直接聊天频道
          subscription = chatService.subscribeDirectChat(
            chatRoomId.value,
            (newMessage) => {
              // 避免重复：只添加不是自己发送的消息（自己发送的已在本地添加）
              if (newMessage.senderId !== currentUser.value?.id) {
                messages.value.push(newMessage)
                scrollToBottom()
              }
            }
          )
        },
        () => {
          console.log('WebSocket connected successfully')
        },
        (error) => {
          console.error('WebSocket error:', error)
          connected.value = false
          message.error('连接失败，请刷新页面重试')
        }
      )
    }

    const loadMessages = async () => {
      try {
        const response = await chatService.getDirectMessages(studentId.value, mentorId.value)
        if (response.code === 0) {
          messages.value = response.data || []
          scrollToBottom()
        }
      } catch (error) {
        console.error('加载消息失败:', error)
      }
    }

    const loadChatPartnerInfo = async () => {
      // 根据当前用户角色确定聊天对象
      if (userRole.value === 'STUDENT') {
        chatPartnerName.value = '导师'
      } else {
        chatPartnerName.value = '学生'
      }
    }

    const handleSendMessage = () => {
      if (!messageInput.value.trim() || !connected.value) {
        return
      }

      sending.value = true
      try {
        const newMessage = {
          chatRoomId: chatRoomId.value,
          studentId: studentId.value,
          mentorId: mentorId.value,
          senderId: currentUser.value.id,
          senderType: userRole.value,
          messageType: 'TEXT',
          content: messageInput.value.trim(),
          timestamp: new Date().toISOString()
        }

        chatService.sendDirectMessage(newMessage)
        
        // 本地添加消息
        messages.value.push({
          ...newMessage,
          createTime: newMessage.timestamp
        })
        
        messageInput.value = ''
        scrollToBottom()
      } catch (error) {
        console.error('发送失败:', error)
        message.error('发送失败')
      } finally {
        sending.value = false
      }
    }

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
        }
      })
    }

    const formatTime = (time) => {
      if (!time) return ''
      return dayjs(time).format('HH:mm')
    }

    onMounted(() => {
      loadChatPartnerInfo()
      loadMessages()
      connectWebSocket()
    })

    onUnmounted(() => {
      if (subscription) {
        subscription.unsubscribe()
      }
      chatService.disconnect()
      connected.value = false
    })

    return {
      messageInput,
      sending,
      messagesContainer,
      messages,
      connected,
      chatPartnerName,
      userRole,
      isSentByMe,
      getSenderName,
      handleSendMessage,
      formatTime
    }
  }
})
</script>

<style scoped>
.direct-chat {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0; /* 关键：允许 flex 子元素收缩 */
  max-height: calc(100vh - 280px); /* 限制最大高度 */
}

.chat-container :deep(.ant-card-body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 8px;
  margin-bottom: 16px;
  min-height: 200px;
  max-height: 100%;
}

.empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.message {
  display: flex;
  margin-bottom: 16px;
}

.message.sent {
  justify-content: flex-end;
}

.message.received {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message.sent .message-content {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: #fff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
  opacity: 0.8;
}

.message-text {
  word-wrap: break-word;
  line-height: 1.5;
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-container .ant-input {
  border-radius: 8px;
}

.input-container .ant-btn {
  height: 72px;
  border-radius: 8px;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .direct-chat {
    padding: 12px;
  }
  
  .message-content {
    max-width: 85%;
  }
  
  .input-container {
    flex-direction: column;
  }
  
  .input-container .ant-btn {
    width: 100%;
    height: 40px;
  }
}
</style>
