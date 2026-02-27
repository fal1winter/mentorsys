<template>
  <div class="chat-room">
    <a-page-header
      title="聊天室"
      :sub-title="applicationInfo?.mentorName"
      @back="() => $router.back()"
    />

    <a-card class="chat-container">
      <div class="messages-container" ref="messagesContainer">
        <div
          v-for="message in messages"
          :key="message.id"
          :class="['message', message.senderType === userRole ? 'sent' : 'received']"
        >
          <div class="message-content">
            <div class="message-header">
              <span class="sender">{{ message.senderType === 'STUDENT' ? '学生' : '导师' }}</span>
              <span class="time">{{ formatTime(message.createTime) }}</span>
            </div>
            <div class="message-text">{{ message.content }}</div>
          </div>
        </div>
      </div>

      <div class="input-container">
        <a-textarea
          v-model:value="messageInput"
          :rows="3"
          placeholder="输入消息..."
          @pressEnter="handleSendMessage"
        />
        <a-button type="primary" @click="handleSendMessage" :loading="sending">
          发送
        </a-button>
      </div>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useStore } from 'vuex'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import chatService from '../../service/chatService'
import applicationService from '../../service/applicationService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'ChatRoom',
  setup() {
    const store = useStore()
    const route = useRoute()
    const messageInput = ref('')
    const sending = ref(false)
    const messagesContainer = ref(null)
    const applicationInfo = ref(null)
    let subscription = null

    const userRole = computed(() => store.getters['auth/userRole'])
    const currentUser = computed(() => store.getters['auth/currentUser'])
    const messages = computed(() =>
      store.getters['chat/getMessagesByApplicationId'](route.params.applicationId)
    )

    const connectWebSocket = () => {
      chatService.connect(
        () => {
          console.log('WebSocket connected')
          store.dispatch('chat/setConnected', true)

          subscription = chatService.subscribe(
            route.params.applicationId,
            (message) => {
              store.dispatch('chat/addMessage', {
                applicationId: route.params.applicationId,
                message
              })
              scrollToBottom()
            }
          )
        },
        () => {
          console.log('WebSocket connected successfully')
        },
        (error) => {
          console.error('WebSocket error:', error)
          message.error('连接失败，请刷新页面重试')
        }
      )
    }

    const loadMessages = async () => {
      try {
        const response = await chatService.getMessages(route.params.applicationId)
        if (response.code === 0) {
          store.dispatch('chat/setMessages', {
            applicationId: route.params.applicationId,
            messages: response.data
          })
          scrollToBottom()
        }
      } catch (error) {
        message.error('加载消息失败')
      }
    }

    const loadApplicationInfo = async () => {
      try {
        const response = await applicationService.getApplicationById(route.params.applicationId)
        if (response.code === 0) {
          applicationInfo.value = response.data
        }
      } catch (error) {
        message.error('加载申请信息失败')
      }
    }

    const handleSendMessage = () => {
      if (!messageInput.value.trim()) {
        return
      }

      sending.value = true
      try {
        const newMessage = {
          applicationId: parseInt(route.params.applicationId),
          senderId: currentUser.value.id,
          senderType: userRole.value,
          messageType: 'TEXT',
          content: messageInput.value,
          isRead: false
        }

        chatService.sendMessage(newMessage)
        messageInput.value = ''
      } catch (error) {
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
      return dayjs(time).format('HH:mm')
    }

    onMounted(() => {
      loadApplicationInfo()
      loadMessages()
      connectWebSocket()
    })

    onUnmounted(() => {
      if (subscription) {
        subscription.unsubscribe()
      }
      chatService.disconnect()
      store.dispatch('chat/setConnected', false)
    })

    return {
      messageInput,
      sending,
      messagesContainer,
      applicationInfo,
      userRole,
      messages,
      handleSendMessage,
      formatTime
    }
  }
})
</script>

<style scoped>
.chat-room {
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
  min-height: 0;
  max-height: calc(100vh - 280px);
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
  border-radius: 4px;
  margin-bottom: 16px;
  min-height: 200px;
  max-height: 100%;
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
  max-width: 60%;
  padding: 12px;
  border-radius: 8px;
  background: #fff;
}

.message.sent .message-content {
  background: #1890ff;
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
}

.input-container {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-container .ant-btn {
  height: 72px;
}
</style>
