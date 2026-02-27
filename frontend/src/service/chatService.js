import axios from './axios'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

class ChatService {
  constructor() {
    this.stompClient = null
    this.connected = false
  }

  connect(onMessageReceived, onConnected, onError) {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: frame => {
        this.connected = true
        // Call onMessageReceived first to set up subscriptions
        if (onMessageReceived) onMessageReceived(frame)
        // Then call onConnected callback
        if (onConnected) onConnected(frame)
      },
      onStompError: error => {
        this.connected = false
        if (onError) onError(error)
      }
    })

    this.stompClient.activate()
  }

  disconnect() {
    if (this.stompClient && this.connected) {
      this.stompClient.deactivate()
      this.connected = false
    }
  }

  subscribe(applicationId, callback) {
    if (this.stompClient && this.connected) {
      return this.stompClient.subscribe(`/topic/chat/${applicationId}`, message => {
        const parsedMessage = JSON.parse(message.body)
        callback(parsedMessage)
      })
    }
  }

  // 订阅直接聊天 - 支持chatRoomId格式
  subscribeDirectChat(chatRoomId, callback) {
    if (this.stompClient && this.connected) {
      return this.stompClient.subscribe(`/topic/direct/${chatRoomId}`, message => {
        const parsedMessage = JSON.parse(message.body)
        callback(parsedMessage)
      })
    }
  }

  sendMessage(message) {
    if (this.stompClient && this.connected) {
      this.stompClient.publish({
        destination: '/app/chat/send',
        body: JSON.stringify(message)
      })
    }
  }

  // 发送直接聊天消息
  sendDirectMessage(message) {
    if (this.stompClient && this.connected) {
      this.stompClient.publish({
        destination: '/app/chat/direct/send',
        body: JSON.stringify(message)
      })
    }
  }

  getMessages(applicationId, page = 1, limit = 50) {
    return axios.get(`/chat/${applicationId}/messages`, {
      params: { page, limit }
    })
  }

  markAsRead(messageId) {
    return axios.put(`/chat/messages/${messageId}/read`)
  }

  getUnreadCount(applicationId, receiverType) {
    return axios.get(`/chat/${applicationId}/unread`, {
      params: { receiverType }
    })
  }

  getDirectMessages(studentId, mentorId, page = 1, limit = 50) {
    return axios.get(`/chat/direct/${studentId}/${mentorId}/messages`, {
      params: { page, limit }
    })
  }

  getDirectUnreadCount(studentId, mentorId, receiverType) {
    return axios.get(`/chat/direct/${studentId}/${mentorId}/unread`, {
      params: { receiverType }
    })
  }

  getConversations(userId, userType) {
    return axios.get('/chat/conversations', {
      params: { userId, userType }
    })
  }

  getTotalUnreadCount(userId, userType) {
    return axios.get('/chat/unread/total', {
      params: { userId, userType }
    })
  }
}

export default new ChatService()
