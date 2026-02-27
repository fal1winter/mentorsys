<template>
  <div class="mentor-detail-container">
    <!-- 导师基本信息区域 - 独立 loading -->
    <a-spin :spinning="loading">
      <div v-if="mentor" class="mentor-detail">
        <a-page-header
          title="导师详情"
          @back="() => $router.back()"
        >
          <template #extra>
            <a-space>
              <a-button
                v-if="isAuthenticated && userRole === 'STUDENT'"
                type="default"
                size="large"
                @click="startChat"
              >
                <MessageOutlined />
                发起聊天
              </a-button>
              <a-button
                v-if="isAuthenticated && userRole === 'STUDENT'"
                type="primary"
                size="large"
                :disabled="!mentor.acceptingStudents"
                @click="showApplicationModal = true"
              >
                申请导师
              </a-button>
            </a-space>
          </template>
        </a-page-header>

        <a-card class="mentor-profile">
          <div class="profile-header">
            <a-avatar :size="120" :src="mentor.avatar">
              {{ mentor.name?.charAt(0) }}
            </a-avatar>
            <div class="profile-info">
              <h1>{{ mentor.name }} <a-tag v-if="mentor.isVerified" color="blue">已认证</a-tag></h1>
              <p class="title-institution">{{ mentor.title }} · {{ mentor.institution }} {{ mentor.department }}</p>
              <div class="stats">
                <a-statistic title="平均评分" :value="(mentor.ratingAvg || 0).toFixed(1)" suffix="/ 5.0">
                  <template #prefix><StarFilled style="color: #faad14" /></template>
                </a-statistic>
                <a-statistic title="学生数" :value="`${mentor.currentStudents || 0}/${mentor.maxStudents || 0}`" />
                <a-statistic title="可招名额" :value="mentor.availablePositions || 0" />
                <a-statistic title="浏览次数" :value="mentor.viewCount || 0" />
              </div>
            </div>
          </div>

          <a-divider />

          <!-- 研究方向 -->
          <div class="section">
            <h3><ExperimentOutlined /> 研究方向</h3>
            <a-space wrap v-if="parseJson(mentor.researchAreas).length">
              <a-tag v-for="area in parseJson(mentor.researchAreas)" :key="area" color="blue">{{ area }}</a-tag>
            </a-space>
            <span v-else class="empty-text">暂无</span>
          </div>

          <!-- 研究关键词 -->
          <div class="section" v-if="parseJson(mentor.keywords).length">
            <h3><TagsOutlined /> 研究关键词</h3>
            <a-space wrap>
              <a-tag v-for="kw in parseJson(mentor.keywords)" :key="kw" color="cyan">{{ kw }}</a-tag>
            </a-space>
          </div>

          <!-- 组内研究方向 -->
          <div class="section" v-if="mentor.groupDirection">
            <h3><TeamOutlined /> 组内研究方向</h3>
            <p>{{ mentor.groupDirection }}</p>
          </div>

          <a-divider />

          <!-- 招生信息 -->
          <a-descriptions title="招生信息" :column="2" bordered size="small">
            <a-descriptions-item label="招生状态">
              <a-tag :color="mentor.acceptingStudents ? 'green' : 'red'">
                {{ mentor.acceptingStudents ? '正在招生' : '暂不招生' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="可招名额">{{ mentor.availablePositions || 0 }} 人</a-descriptions-item>
            <a-descriptions-item label="期望学生素质" :span="2">
              {{ mentor.expectedStudentQualities || '暂无要求' }}
            </a-descriptions-item>
            <a-descriptions-item label="指导风格" :span="2">
              {{ mentor.mentoringStyle || '暂无说明' }}
            </a-descriptions-item>
            <a-descriptions-item label="经费情况" :span="2">
              {{ mentor.fundingStatus || '暂无说明' }}
            </a-descriptions-item>
            <a-descriptions-item label="合作机会" :span="2" v-if="mentor.collaborationOpportunities">
              {{ mentor.collaborationOpportunities }}
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <!-- 联系方式 -->
          <a-descriptions title="联系方式" :column="2" bordered size="small">
            <a-descriptions-item label="邮箱">{{ mentor.email || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="电话">{{ mentor.phone || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="办公室">{{ mentor.officeLocation || '暂无' }}</a-descriptions-item>
            <a-descriptions-item label="个人主页">
              <a v-if="mentor.homepageUrl" :href="mentor.homepageUrl" target="_blank">{{ mentor.homepageUrl }}</a>
              <span v-else>暂无</span>
            </a-descriptions-item>
            <a-descriptions-item label="Google Scholar" :span="2">
              <a v-if="mentor.googleScholarUrl" :href="mentor.googleScholarUrl" target="_blank">{{ mentor.googleScholarUrl }}</a>
              <span v-else>暂无</span>
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <!-- 教育背景 -->
          <div class="section" v-if="mentor.educationBackground">
            <h3><ReadOutlined /> 教育背景</h3>
            <p style="white-space: pre-line">{{ mentor.educationBackground }}</p>
          </div>

          <!-- 个人简介 -->
          <div class="section">
            <h3><UserOutlined /> 个人简介</h3>
            <p style="white-space: pre-line">{{ mentor.bio || '暂无简介' }}</p>
          </div>
        </a-card>
      </div>
    </a-spin>

    <!-- 作品和评价区域 - 并行加载，不受导师信息 loading 影响 -->
    <a-card class="publications-card">
      <MentorPublicationList
        :mentorId="parseInt(route.params.id)"
        :canManage="isAuthenticated && (userRole === 'ADMIN' || (userRole === 'MENTOR' && mentor && currentUser && mentor.userId === currentUser.id))"
      />
    </a-card>

    <a-card class="ratings-card">
      <MentorRatingCreate
        v-if="isAuthenticated"
        :mentorId="parseInt(route.params.id)"
        @refresh="handleRatingRefresh"
      />
      <MentorRatingList
        ref="ratingListRef"
        :mentorId="parseInt(route.params.id)"
      />
    </a-card>

    <a-modal
      v-model:visible="showApplicationModal"
      title="申请导师"
      @ok="handleApply"
      :confirm-loading="applyLoading"
      width="700px"
    >
      <a-form layout="vertical">
        <a-form-item label="申请信" required>
          <a-textarea
            v-model:value="applicationForm.applicationLetter"
            :rows="4"
            placeholder="请介绍您的学术背景和研究兴趣..."
          />
        </a-form-item>

        <a-form-item label="研究计划" required>
          <a-textarea
            v-model:value="applicationForm.researchProposal"
            :rows="4"
            placeholder="请简要说明您的研究计划和目标..."
          />
        </a-form-item>

        <a-form-item label="给导师的留言">
          <a-textarea
            v-model:value="applicationForm.studentMessage"
            :rows="3"
            placeholder="您想对导师说的话..."
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { StarFilled, MessageOutlined, ExperimentOutlined, TagsOutlined, TeamOutlined, ReadOutlined, UserOutlined } from '@ant-design/icons-vue'
import mentorService from '../../service/mentorService'
import applicationService from '../../service/applicationService'
import studentService from '../../service/studentService'
import MentorPublicationList from '../publication/MentorPublicationList.vue'
import MentorRatingCreate from '../rating/MentorRatingCreate.vue'
import MentorRatingList from '../rating/MentorRatingList.vue'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'MentorDetail',
  components: {
    StarFilled,
    MessageOutlined,
    ExperimentOutlined,
    TagsOutlined,
    TeamOutlined,
    ReadOutlined,
    UserOutlined,
    MentorPublicationList,
    MentorRatingCreate,
    MentorRatingList
  },
  setup() {
    const store = useStore()
    const route = useRoute()
    const router = useRouter()
    const loading = ref(false)
    const applyLoading = ref(false)
    const showApplicationModal = ref(false)
    const applicationForm = ref({
      applicationLetter: '',
      researchProposal: '',
      studentMessage: ''
    })
    const ratingListRef = ref()

    const isAuthenticated = computed(() => store.getters['auth/isAuthenticated'])
    const userRole = computed(() => store.getters['auth/userRole'])
    const currentUser = computed(() => store.getters['auth/currentUser'])
    const mentor = computed(() => store.getters['mentor/currentMentor'])

    const fetchMentorDetail = async () => {
      loading.value = true
      try {
        await store.dispatch('mentor/fetchMentorById', route.params.id)
      } catch (error) {
        message.error('获取导师信息失败')
      } finally {
        loading.value = false
      }
    }

    const startChat = async () => {
      if (!currentUser.value?.id || !mentor.value?.id) {
        message.warning('请先登录')
        return
      }
      try {
        // 获取当前学生的 studentId
        const response = await studentService.getStudentByUserId(currentUser.value.id)
        if (response.code === 0 && response.data) {
          router.push(`/chat/direct/${response.data.id}/${mentor.value.id}`)
        } else {
          message.warning('请先完善学生信息')
        }
      } catch (error) {
        console.error('获取学生信息失败:', error)
        message.error('无法发起聊天')
      }
    }

    const handleRatingRefresh = () => {
      if (ratingListRef.value) {
        ratingListRef.value.refresh()
      }
    }

    const handleApply = async () => {
      if (!applicationForm.value.applicationLetter.trim()) {
        message.warning('请填写申请信')
        return
      }
      if (!applicationForm.value.researchProposal.trim()) {
        message.warning('请填写研究计划')
        return
      }

      applyLoading.value = true
      try {
        const response = await applicationService.createApplication({
          mentorId: parseInt(route.params.id),
          studentId: currentUser.value.id,
          applicationLetter: applicationForm.value.applicationLetter,
          researchProposal: applicationForm.value.researchProposal,
          studentMessage: applicationForm.value.studentMessage
        })

        if (response.code === 0) {
          message.success('申请已提交，请等待导师审核')
          showApplicationModal.value = false
          applicationForm.value = {
            applicationLetter: '',
            researchProposal: '',
            studentMessage: ''
          }
        } else {
          message.error(response.message || '申请失败')
        }
      } catch (error) {
        message.error('申请失败')
      } finally {
        applyLoading.value = false
      }
    }

    const formatDate = (date) => {
      return dayjs(date).format('YYYY-MM-DD')
    }

    const parseJson = (value) => {
      if (!value) return []
      if (Array.isArray(value)) return value
      try {
        const parsed = JSON.parse(value)
        return Array.isArray(parsed) ? parsed : []
      } catch {
        return typeof value === 'string' ? value.split(',').map(s => s.trim()).filter(s => s) : []
      }
    }

    onMounted(() => {
      fetchMentorDetail()
    })

    return {
      loading,
      applyLoading,
      showApplicationModal,
      applicationForm,
      ratingListRef,
      isAuthenticated,
      userRole,
      currentUser,
      mentor,
      route,
      handleApply,
      handleRatingRefresh,
      formatDate,
      startChat,
      parseJson
    }
  }
})
</script>

<style scoped>
.mentor-detail-container { max-width: 1200px; margin: 0 auto; padding: 24px; }
.mentor-profile { margin-bottom: 24px; }
.profile-header { display: flex; gap: 24px; align-items: flex-start; }
.profile-info { flex: 1; }
.profile-info h1 { margin: 0 0 8px 0; font-size: 28px; }
.title-institution { color: #666; font-size: 16px; margin-bottom: 16px; }
.stats { display: flex; gap: 32px; flex-wrap: wrap; }
.section { margin-top: 24px; }
.section h3 { margin-bottom: 12px; color: #1890ff; }
.empty-text { color: #999; font-style: italic; }
.publications-card, .ratings-card { margin-bottom: 24px; }
@media screen and (max-width: 768px) {
  .profile-header { flex-direction: column; align-items: center; text-align: center; }
  .stats { justify-content: center; }
}
</style>
