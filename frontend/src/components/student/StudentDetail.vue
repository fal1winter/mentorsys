<template>
  <div class="student-detail-page">
    <a-spin :spinning="loading">
      <div v-if="student" class="detail-container">
        <!-- 头部信息 -->
        <div class="profile-header" :style="{ background: getGradient(student.id) }">
          <a-avatar :size="100" :src="student.avatar" class="profile-avatar">
            {{ student.name?.charAt(0) }}
          </a-avatar>
          <div class="profile-info">
            <h1>{{ student.name }}</h1>
            <p class="subtitle">
              {{ student.currentInstitution || '未设置学校' }}
              <span v-if="student.major"> · {{ student.major }}</span>
              <a-tag v-if="student.degreeLevel" color="white" style="margin-left: 8px;">
                {{ student.degreeLevel }}
              </a-tag>
            </p>
            <div class="contact-info">
              <span v-if="student.email"><MailOutlined /> {{ student.email }}</span>
              <span v-if="student.phone"><PhoneOutlined /> {{ student.phone }}</span>
            </div>
          </div>
          <div class="profile-actions" v-if="isMentor">
            <a-button type="primary" size="large" @click="startChat">
              <MessageOutlined /> 发送消息
            </a-button>
          </div>
        </div>

        <!-- 基本信息 -->
        <a-card title="基本信息" class="info-card">
          <a-descriptions :column="{ xs: 1, sm: 2, md: 3 }" bordered>
            <a-descriptions-item label="学校">{{ student.currentInstitution || '-' }}</a-descriptions-item>
            <a-descriptions-item label="专业">{{ student.major || '-' }}</a-descriptions-item>
            <a-descriptions-item label="学位">{{ student.degreeLevel || '-' }}</a-descriptions-item>
            <a-descriptions-item label="预计毕业">{{ student.graduationYear || '-' }}</a-descriptions-item>
            <a-descriptions-item label="GPA">{{ student.gpa || '-' }}</a-descriptions-item>
            <a-descriptions-item label="发表论文">{{ student.publicationsCount || 0 }} 篇</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 研究兴趣 -->
        <a-card title="研究兴趣" class="info-card" v-if="student.researchInterests">
          <div class="tags-container">
            <a-tag 
              v-for="interest in parseJsonArray(student.researchInterests)" 
              :key="interest"
              color="blue"
              size="large"
            >
              {{ interest }}
            </a-tag>
          </div>
        </a-card>

        <!-- 期望研究方向 -->
        <a-card title="期望研究方向" class="info-card" v-if="student.expectedResearchDirection">
          <p class="text-content">{{ student.expectedResearchDirection }}</p>
        </a-card>

        <!-- 个人能力 -->
        <a-card title="个人能力" class="info-card" v-if="student.personalAbilities">
          <p class="text-content">{{ student.personalAbilities }}</p>
        </a-card>

        <!-- 编程技能 -->
        <a-card title="编程技能" class="info-card" v-if="student.programmingSkills">
          <div class="tags-container">
            <a-tag 
              v-for="skill in parseJsonArray(student.programmingSkills)" 
              :key="skill"
              color="green"
              size="large"
            >
              {{ skill }}
            </a-tag>
          </div>
        </a-card>

        <!-- 项目经验 -->
        <a-card title="项目经验" class="info-card" v-if="student.projectExperience">
          <p class="text-content">{{ student.projectExperience }}</p>
        </a-card>

        <!-- 个人简介 -->
        <a-card title="个人简介" class="info-card" v-if="student.bio">
          <p class="text-content">{{ student.bio }}</p>
        </a-card>

        <!-- 期望导师风格 -->
        <a-card title="期望导师风格" class="info-card" v-if="student.preferredMentorStyle">
          <p class="text-content">{{ student.preferredMentorStyle }}</p>
        </a-card>

        <!-- 简历 -->
        <a-card title="简历" class="info-card" v-if="student.cvUrl">
          <a-button type="primary" :href="student.cvUrl" target="_blank">
            <FileTextOutlined /> 查看简历
          </a-button>
        </a-card>
      </div>
      <a-empty v-else-if="!loading" description="学生不存在" />
    </a-spin>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { MailOutlined, PhoneOutlined, MessageOutlined, FileTextOutlined } from '@ant-design/icons-vue'
import studentService from '@/service/studentService'

export default defineComponent({
  name: 'StudentDetail',
  components: {
    MailOutlined, PhoneOutlined, MessageOutlined, FileTextOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const route = useRoute()
    const loading = ref(false)
    const student = ref(null)

    const isMentor = computed(() => store.getters['auth/userRole'] === 'MENTOR')
    const currentUser = computed(() => store.getters['auth/currentUser'])

    const gradients = [
      'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
      'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
      'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
    ]

    const getGradient = (id) => gradients[id % gradients.length]

    const parseJsonArray = (jsonStr) => {
      if (!jsonStr) return []
      try {
        const parsed = JSON.parse(jsonStr)
        return Array.isArray(parsed) ? parsed : []
      } catch {
        return []
      }
    }

    const fetchStudent = async () => {
      const studentId = route.params.id
      if (!studentId) return

      loading.value = true
      try {
        const response = await studentService.getProfile(studentId)
        if (response.code === 0) {
          student.value = response.data
        } else {
          message.error(response.message || '获取学生信息失败')
        }
      } catch (error) {
        console.error('获取学生信息失败:', error)
        message.error('获取学生信息失败')
      } finally {
        loading.value = false
      }
    }

    const startChat = () => {
      if (!currentUser.value) {
        message.warning('请先登录')
        return
      }
      router.push(`/chat/direct/${student.value.userId}/${currentUser.value.id}`)
    }

    onMounted(() => {
      fetchStudent()
    })

    return {
      loading, student, isMentor,
      getGradient, parseJsonArray, startChat
    }
  }
})
</script>

<style scoped>
.student-detail-page {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 32px;
  border-radius: 12px;
  color: #fff;
  margin-bottom: 24px;
}

.profile-avatar {
  border: 4px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  flex-shrink: 0;
}

.profile-info {
  flex: 1;
}

.profile-info h1 {
  margin: 0 0 8px 0;
  color: #fff;
  font-size: 28px;
}

.profile-info .subtitle {
  margin: 0 0 12px 0;
  opacity: 0.9;
  font-size: 16px;
}

.contact-info {
  display: flex;
  gap: 24px;
  opacity: 0.9;
}

.contact-info span {
  display: flex;
  align-items: center;
  gap: 6px;
}

.profile-actions {
  flex-shrink: 0;
}

.info-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.info-card :deep(.ant-card-head-title) {
  color: #1890ff;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.text-content {
  color: #666;
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
}
</style>
