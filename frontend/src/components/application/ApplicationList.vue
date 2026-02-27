<template>
  <div class="application-list-container">
    <div class="page-header">
      <div class="header-content">
        <h2>📋 申请管理</h2>
        <p class="header-desc">查看和管理您的师生匹配申请</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange" class="app-tabs">
      <a-tab-pane key="all" tab="全部申请" />
      <a-tab-pane key="pending" tab="待处理" />
      <a-tab-pane key="accepted" tab="已接受" />
      <a-tab-pane key="rejected" tab="已拒绝" />
    </a-tabs>

    <a-spin :spinning="loading">
      <div v-if="applications.length > 0" class="application-grid">
        <div v-for="item in applications" :key="item.id" class="app-card">
          <div class="app-card-header">
            <div class="partner-info">
              <a-avatar :size="48" class="partner-avatar">
                {{ getPartnerName(item)?.charAt(0) || '?' }}
              </a-avatar>
              <div class="partner-detail">
                <div class="partner-name">
                  <router-link v-if="userRole === 'STUDENT' && item.mentor" :to="`/mentors/${item.mentorId}`">
                    {{ item.mentor?.name || '未知导师' }}
                  </router-link>
                  <router-link v-else-if="userRole === 'MENTOR' && item.student" :to="`/students/${item.studentId}`">
                    {{ item.student?.name || '未知学生' }}
                  </router-link>
                  <span v-else>{{ getPartnerName(item) }}</span>
                </div>
                <div class="partner-meta">
                  <template v-if="userRole === 'STUDENT' && item.mentor">
                    <a-tag color="blue" size="small">导师</a-tag>
                    <span v-if="item.mentor.title">{{ item.mentor.title }}</span>
                    <span v-if="item.mentor.institution"> · {{ item.mentor.institution }}</span>
                  </template>
                  <template v-else-if="userRole === 'MENTOR' && item.student">
                    <a-tag color="green" size="small">学生</a-tag>
                    <span v-if="item.student.major">{{ item.student.major }}</span>
                    <span v-if="item.student.institution"> · {{ item.student.institution }}</span>
                  </template>
                </div>
              </div>
            </div>
            <a-tag :color="getStatusColor(item.status)" class="status-tag">
              {{ getStatusText(item.status) }}
            </a-tag>
          </div>

          <!-- 对方详细信息 -->
          <div class="partner-extra-info" v-if="userRole === 'STUDENT' && item.mentor">
            <div class="info-row" v-if="item.mentor.researchAreas">
              <span class="info-label">研究方向：</span>
              <span>{{ item.mentor.researchAreas }}</span>
            </div>
            <div class="info-row" v-if="item.mentor.department">
              <span class="info-label">院系：</span>
              <span>{{ item.mentor.department }}</span>
            </div>
          </div>
          <div class="partner-extra-info" v-else-if="userRole === 'MENTOR' && item.student">
            <div class="info-row" v-if="item.student.researchInterests">
              <span class="info-label">研究兴趣：</span>
              <span>{{ item.student.researchInterests }}</span>
            </div>
            <div class="info-row" v-if="item.student.gpa">
              <span class="info-label">GPA：</span>
              <span>{{ item.student.gpa }}</span>
            </div>
          </div>

          <div class="app-card-body">
            <div v-if="item.applicationLetter" class="app-field">
              <span class="field-label">📝 申请信：</span>
              <span class="field-value">{{ item.applicationLetter }}</span>
            </div>
            <div v-if="item.researchProposal" class="app-field">
              <span class="field-label">🔬 研究计划：</span>
              <span class="field-value">{{ item.researchProposal }}</span>
            </div>
            <div v-if="item.studentMessage" class="app-field">
              <span class="field-label">💬 学生留言：</span>
              <span class="field-value">{{ item.studentMessage }}</span>
            </div>
            <div v-if="item.mentorFeedback" class="app-field">
              <span class="field-label">💡 导师反馈：</span>
              <span class="field-value">{{ item.mentorFeedback }}</span>
            </div>
          </div>

          <div class="app-card-footer">
            <span class="apply-time">申请时间: {{ formatDate(item.createTime || item.applyTime) }}</span>
            <div class="action-buttons">
              <a-button
                v-if="item.status === 'accepted'"
                type="primary"
                size="small"
                @click="openChat(item)"
              >
                💬 进入聊天
              </a-button>
              <a-button
                v-if="userRole === 'STUDENT' && item.status === 'pending'"
                danger
                size="small"
                @click="handleWithdraw(item.id)"
              >
                撤回
              </a-button>
              <a-button
                v-if="userRole === 'MENTOR' && item.status === 'pending'"
                type="primary"
                size="small"
                @click="showAcceptModal(item)"
              >
                接受
              </a-button>
              <a-button
                v-if="userRole === 'MENTOR' && item.status === 'pending'"
                danger
                size="small"
                @click="showRejectModal(item)"
              >
                拒绝
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无申请记录" />
    </a-spin>

    <a-modal v-model:visible="acceptModalVisible" title="接受申请" @ok="handleAccept" :confirm-loading="actionLoading">
      <a-form layout="vertical">
        <a-form-item label="反馈信息">
          <a-textarea v-model:value="feedback" :rows="4" placeholder="请输入反馈信息（可选）" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:visible="rejectModalVisible" title="拒绝申请" @ok="handleReject" :confirm-loading="actionLoading">
      <a-form layout="vertical">
        <a-form-item label="拒绝理由">
          <a-textarea v-model:value="feedback" :rows="4" placeholder="请说明拒绝理由" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import applicationService from '../../service/applicationService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'ApplicationList',
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const actionLoading = ref(false)
    const activeTab = ref('all')
    const applications = ref([])
    const acceptModalVisible = ref(false)
    const rejectModalVisible = ref(false)
    const currentApplication = ref(null)
    const feedback = ref('')

    const userRole = computed(() => store.getters['auth/userRole'])
    const currentUser = computed(() => store.getters['auth/currentUser'])

    const getPartnerName = (item) => {
      if (userRole.value === 'STUDENT') {
        return item.mentor?.name || item.mentorName || '未知导师'
      } else {
        return item.student?.name || item.studentName || '未知学生'
      }
    }

    const fetchApplications = async () => {
      loading.value = true
      try {
        const params = {}
        if (userRole.value === 'STUDENT') {
          params.studentId = currentUser.value.id
        } else if (userRole.value === 'MENTOR') {
          params.mentorId = currentUser.value.id
        }
        if (activeTab.value !== 'all') {
          params.status = activeTab.value
        }
        const response = await applicationService.getApplications(params)
        if (response.code === 0) {
          applications.value = response.data || []
        }
      } catch (error) {
        message.error('获取申请列表失败')
      } finally {
        loading.value = false
      }
    }

    const openChat = (item) => {
      // 使用直接聊天路由，确保消息统一
      router.push(`/chat/direct/${item.studentId}/${item.mentorId}`)
    }

    const handleTabChange = () => { fetchApplications() }

    const handleWithdraw = (id) => {
      Modal.confirm({
        title: '确认撤回申请？',
        content: '撤回后将无法恢复',
        onOk: async () => {
          try {
            const response = await applicationService.withdrawApplication(id)
            if (response.code === 0) {
              message.success('已撤回申请')
              fetchApplications()
            }
          } catch (error) { message.error('撤回失败') }
        }
      })
    }

    const showAcceptModal = (app) => { currentApplication.value = app; feedback.value = ''; acceptModalVisible.value = true }
    const showRejectModal = (app) => { currentApplication.value = app; feedback.value = ''; rejectModalVisible.value = true }

    const handleAccept = async () => {
      actionLoading.value = true
      try {
        const response = await applicationService.acceptApplication(currentApplication.value.id, feedback.value)
        if (response.code === 0) { message.success('已接受申请'); acceptModalVisible.value = false; fetchApplications() }
        else { message.error(response.message || '操作失败') }
      } catch (error) { message.error('操作失败') }
      finally { actionLoading.value = false }
    }

    const handleReject = async () => {
      if (!feedback.value.trim()) { message.warning('请填写拒绝理由'); return }
      actionLoading.value = true
      try {
        const response = await applicationService.rejectApplication(currentApplication.value.id, feedback.value)
        if (response.code === 0) { message.success('已拒绝申请'); rejectModalVisible.value = false; fetchApplications() }
        else { message.error(response.message || '操作失败') }
      } catch (error) { message.error('操作失败') }
      finally { actionLoading.value = false }
    }

    const getStatusColor = (status) => ({ pending: 'processing', accepted: 'success', rejected: 'error', withdrawn: 'default' }[status] || 'default')
    const getStatusText = (status) => ({ pending: '待处理', accepted: '已接受', rejected: '已拒绝', withdrawn: '已撤回' }[status] || status)
    const formatDate = (date) => date ? dayjs(date).format('YYYY-MM-DD HH:mm') : ''

    onMounted(() => { fetchApplications() })

    return {
      loading, actionLoading, activeTab, applications,
      acceptModalVisible, rejectModalVisible, feedback, userRole,
      getPartnerName, handleTabChange, handleWithdraw,
      showAcceptModal, showRejectModal, handleAccept, handleReject,
      getStatusColor, getStatusText, formatDate, openChat
    }
  }
})
</script>

<style scoped>
.application-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}
.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  padding: 24px 32px;
  margin-bottom: 24px;
  color: #fff;
}
.page-header h2 { margin: 0; color: #fff; font-size: 24px; }
.header-desc { margin: 4px 0 0; opacity: 0.85; }
.app-tabs { margin-bottom: 20px; }
.application-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.app-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transition: all 0.3s;
}
.app-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
  transform: translateY(-2px);
}
.app-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.partner-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.partner-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-weight: bold;
}
.partner-name { font-size: 16px; font-weight: 600; }
.partner-name a { color: #1890ff; }
.partner-meta { font-size: 13px; color: #666; margin-top: 2px; display: flex; align-items: center; gap: 4px; }
.status-tag { font-size: 13px; }
.partner-extra-info {
  background: #f8f9ff;
  border-radius: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
}
.info-row { font-size: 13px; color: #555; margin-bottom: 4px; }
.info-row:last-child { margin-bottom: 0; }
.info-label { color: #888; }
.app-card-body { margin-bottom: 12px; }
.app-field { margin-bottom: 8px; font-size: 14px; }
.field-label { color: #888; }
.field-value { color: #333; }
.app-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.apply-time { color: #999; font-size: 12px; }
.action-buttons { display: flex; gap: 8px; }
</style>
