<template>
  <div class="my-students-container">
    <div class="page-header">
      <div class="header-left">
        <h2><TeamOutlined /> 我的学生</h2>
        <p>查看已接受申请的学生，与他们保持联系</p>
      </div>
      <div class="header-stats">
        <a-statistic title="学生总数" :value="total" />
      </div>
    </div>

    <a-spin :spinning="loading">
      <div v-if="students.length > 0" class="student-grid">
        <div v-for="student in students" :key="student.id" class="student-card">
          <div class="card-cover" :style="{ background: getGradient(student.id) }">
            <a-avatar :size="72" :src="student.avatar" class="card-avatar">
              {{ student.name?.charAt(0) }}
            </a-avatar>
            <div class="card-badge" v-if="student.degreeLevel">
              {{ student.degreeLevel }}
            </div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ student.name }}</h3>
            <p class="card-subtitle">
              {{ student.currentInstitution || '未设置学校' }}
              <span v-if="student.major"> · {{ student.major }}</span>
            </p>
            
            <div class="card-info">
              <div class="info-row" v-if="student.email">
                <MailOutlined />
                <span>{{ student.email }}</span>
              </div>
              <div class="info-row" v-if="student.gpa">
                <TrophyOutlined />
                <span>GPA: {{ student.gpa }}</span>
              </div>
              <div class="info-row" v-if="student.graduationYear">
                <CalendarOutlined />
                <span>预计 {{ student.graduationYear }} 年毕业</span>
              </div>
              <div class="info-row" v-if="student.publicationsCount">
                <FileTextOutlined />
                <span>{{ student.publicationsCount }} 篇论文</span>
              </div>
            </div>

            <div class="card-tags" v-if="student.researchInterests">
              <a-tag 
                v-for="interest in parseJsonArray(student.researchInterests).slice(0, 3)" 
                :key="interest"
                color="blue"
              >
                {{ interest }}
              </a-tag>
            </div>

            <div class="card-skills" v-if="student.programmingSkills">
              <a-tag 
                v-for="skill in parseJsonArray(student.programmingSkills).slice(0, 4)" 
                :key="skill"
                color="green"
                size="small"
              >
                {{ skill }}
              </a-tag>
            </div>
          </div>
          <div class="card-footer">
            <a-button type="link" size="small" @click="handleViewProfile(student)">
              <EyeOutlined /> 查看详情
            </a-button>
            <a-button type="link" size="small" @click="handleChat(student)">
              <MessageOutlined /> 发消息
            </a-button>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无学生" />
    </a-spin>

    <!-- 分页 -->
    <div class="pagination" v-if="total > pageSize">
      <a-pagination
        v-model:current="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :show-total="total => `共 ${total} 名学生`"
        show-size-changer
        @change="handlePageChange"
      />
    </div>

    <!-- 学生详情模态框 -->
    <a-modal
      v-model:visible="profileModalVisible"
      :title="selectedStudent?.name + ' - 详细信息'"
      width="750px"
      :footer="null"
    >
      <div v-if="selectedStudent" class="student-detail">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="姓名" :span="1">{{ selectedStudent.name }}</a-descriptions-item>
          <a-descriptions-item label="邮箱" :span="1">{{ selectedStudent.email || '-' }}</a-descriptions-item>
          <a-descriptions-item label="电话" :span="1">{{ selectedStudent.phone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="学位" :span="1">{{ selectedStudent.degreeLevel || '-' }}</a-descriptions-item>
          <a-descriptions-item label="学校" :span="1">{{ selectedStudent.currentInstitution || '-' }}</a-descriptions-item>
          <a-descriptions-item label="专业" :span="1">{{ selectedStudent.major || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预计毕业" :span="1">{{ selectedStudent.graduationYear || '-' }}</a-descriptions-item>
          <a-descriptions-item label="GPA" :span="1">{{ selectedStudent.gpa || '-' }}</a-descriptions-item>
          <a-descriptions-item label="发表论文数" :span="2">{{ selectedStudent.publicationsCount || 0 }}</a-descriptions-item>
        </a-descriptions>

        <a-divider>研究信息</a-divider>

        <div class="info-section" v-if="selectedStudent.researchInterests">
          <h4>研究兴趣</h4>
          <div class="tags">
            <a-tag v-for="interest in parseJsonArray(selectedStudent.researchInterests)" :key="interest" color="blue">
              {{ interest }}
            </a-tag>
          </div>
        </div>

        <div class="info-section" v-if="selectedStudent.expectedResearchDirection">
          <h4>期望研究方向</h4>
          <p>{{ selectedStudent.expectedResearchDirection }}</p>
        </div>

        <div class="info-section" v-if="selectedStudent.personalAbilities">
          <h4>个人能力</h4>
          <p>{{ selectedStudent.personalAbilities }}</p>
        </div>

        <div class="info-section" v-if="selectedStudent.programmingSkills">
          <h4>编程技能</h4>
          <div class="tags">
            <a-tag v-for="skill in parseJsonArray(selectedStudent.programmingSkills)" :key="skill" color="green">
              {{ skill }}
            </a-tag>
          </div>
        </div>

        <div class="info-section" v-if="selectedStudent.projectExperience">
          <h4>项目经验</h4>
          <p>{{ selectedStudent.projectExperience }}</p>
        </div>

        <div class="info-section" v-if="selectedStudent.bio">
          <h4>个人简介</h4>
          <p>{{ selectedStudent.bio }}</p>
        </div>

        <div class="info-section" v-if="selectedStudent.preferredMentorStyle">
          <h4>期望导师风格</h4>
          <p>{{ selectedStudent.preferredMentorStyle }}</p>
        </div>

        <div v-if="selectedStudent.cvUrl" class="cv-section">
          <a-button type="primary" :href="selectedStudent.cvUrl" target="_blank">
            <FileTextOutlined /> 查看简历
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, watch } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  TeamOutlined, MailOutlined, TrophyOutlined, CalendarOutlined,
  FileTextOutlined, MessageOutlined, EyeOutlined
} from '@ant-design/icons-vue'
import studentService from '../../service/studentService'
import mentorService from '../../service/mentorService'

export default defineComponent({
  name: 'MyStudents',
  components: {
    TeamOutlined, MailOutlined, TrophyOutlined, CalendarOutlined,
    FileTextOutlined, MessageOutlined, EyeOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const students = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(12)
    const profileModalVisible = ref(false)
    const selectedStudent = ref(null)
    const mentorRecord = ref(null)

    const currentUser = computed(() => store.getters['auth/currentUser'])
    const mentorId = computed(() => mentorRecord.value?.id)

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

    const fetchMentorRecord = async () => {
      if (!currentUser.value?.id) return

      try {
        const response = await mentorService.getMentorByUserId(currentUser.value.id)
        if (response.code === 0) {
          mentorRecord.value = response.data
        } else {
          message.error(response.message || '获取导师信息失败')
        }
      } catch (error) {
        console.error('获取导师信息失败:', error)
        message.error('获取导师信息失败')
      }
    }

    const fetchStudents = async () => {
      if (!mentorId.value) return

      loading.value = true
      try {
        const response = await studentService.getStudentsByMentor(
          mentorId.value,
          currentPage.value,
          pageSize.value
        )

        if (response.code === 0) {
          students.value = response.data || []
          total.value = response.total || 0
        } else {
          message.error(response.message || '获取学生列表失败')
        }
      } catch (error) {
        console.error('获取学生列表失败:', error)
        message.error('获取学生列表失败')
      } finally {
        loading.value = false
      }
    }

    const handlePageChange = (page, size) => {
      currentPage.value = page
      pageSize.value = size
      fetchStudents()
    }

    const handleChat = (student) => {
      router.push(`/chat/direct/${student.userId}/${mentorId.value}`)
    }

    const handleViewProfile = (student) => {
      selectedStudent.value = student
      profileModalVisible.value = true
    }

    onMounted(async () => {
      await fetchMentorRecord()
      if (mentorId.value) {
        fetchStudents()
      }
    })

    watch(mentorId, (newValue, oldValue) => {
      if (newValue && !oldValue) {
        fetchStudents()
      }
    })

    return {
      loading, students, total, currentPage, pageSize,
      profileModalVisible, selectedStudent,
      getGradient, parseJsonArray,
      handlePageChange, handleChat, handleViewProfile
    }
  }
})
</script>

<style scoped>
.my-students-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
}

.header-left h2 {
  margin: 0 0 8px 0;
  color: #fff;
  font-size: 24px;
}

.header-left p {
  margin: 0;
  opacity: 0.9;
}

.header-stats :deep(.ant-statistic-title) {
  color: rgba(255, 255, 255, 0.85);
}

.header-stats :deep(.ant-statistic-content) {
  color: #fff;
}

.student-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.student-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.student-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
}

.card-cover {
  position: relative;
  padding: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.card-avatar {
  border: 3px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.card-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.9);
  color: #1890ff;
}

.card-body {
  padding: 16px 20px;
}

.card-title {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.card-subtitle {
  margin: 0 0 12px 0;
  color: #666;
  font-size: 13px;
}

.card-info {
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
  color: #666;
  font-size: 13px;
}

.info-row .anticon {
  color: #1890ff;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.card-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* 详情弹窗样式 */
.student-detail {
  padding: 16px 0;
}

.info-section {
  margin-bottom: 16px;
}

.info-section h4 {
  color: #1890ff;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-section p {
  color: #666;
  line-height: 1.6;
  margin: 0;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cv-section {
  margin-top: 24px;
  text-align: center;
}
</style>
