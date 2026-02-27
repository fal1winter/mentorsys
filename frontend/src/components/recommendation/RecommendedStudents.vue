<template>
  <div class="recommendations-container">
    <a-page-header
      title="学生推荐"
      sub-title="基于您的研究方向为您推荐合适的学生"
      @back="() => $router.push('/mentors')"
    >
      <template #extra>
        <a-space>
          <a-button @click="showEditModal = true" type="default">
            <EditOutlined />
            编辑招生需求
          </a-button>
          <a-button @click="fetchRecommendations" :loading="loading" type="primary">
            <ReloadOutlined :spin="loading" />
            刷新推荐
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 导师招生需求展示卡片 -->
    <a-card class="preferences-card">
      <template #title>
        <BulbOutlined /> 您的招生需求
      </template>
      <template #extra>
        <a-button type="link" @click="showEditModal = true">
          <EditOutlined /> 编辑需求
        </a-button>
      </template>
      <a-spin :spinning="loadingMentor">
        <div v-if="mentorInfo">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="研究方向" :span="2">
              <a-space wrap v-if="parseJson(mentorInfo.researchAreas).length">
                <a-tag v-for="area in parseJson(mentorInfo.researchAreas)" :key="area" color="blue">{{ area }}</a-tag>
              </a-space>
              <span v-else class="empty-text">未填写 <a @click="showEditModal = true">点击填写</a></span>
            </a-descriptions-item>
            <a-descriptions-item label="组内研究方向" :span="2">
              <span v-if="mentorInfo.groupDirection">{{ mentorInfo.groupDirection }}</span>
              <span v-else class="empty-text">未填写</span>
            </a-descriptions-item>
            <a-descriptions-item label="期望学生素质" :span="2">
              <span v-if="mentorInfo.expectedStudentQualities">{{ mentorInfo.expectedStudentQualities }}</span>
              <span v-else class="empty-text">未填写 <a @click="showEditModal = true">点击填写</a></span>
            </a-descriptions-item>
            <a-descriptions-item label="指导风格">
              <span v-if="mentorInfo.mentoringStyle">{{ mentorInfo.mentoringStyle }}</span>
              <span v-else class="empty-text">未填写</span>
            </a-descriptions-item>
            <a-descriptions-item label="招生状态">
              <a-tag :color="mentorInfo.acceptingStudents ? 'green' : 'red'">
                {{ mentorInfo.acceptingStudents ? '正在招生' : '暂不招生' }}
              </a-tag>
              <span style="margin-left: 8px">可招 {{ mentorInfo.availablePositions || 0 }} 人</span>
            </a-descriptions-item>
          </a-descriptions>
        </div>
        <a-empty v-else description="暂无导师信息，请先填写您的招生需求">
          <template #extra>
            <a-button type="primary" @click="showEditModal = true">
              <EditOutlined /> 填写招生需求
            </a-button>
          </template>
        </a-empty>
      </a-spin>
    </a-card>

    <!-- 推荐学生列表 -->
    <a-card class="recommendations-card">
      <template #title>
        <span><UserOutlined /> 推荐学生</span>
        <a-tag color="purple" style="margin-left: 8px">{{ recommendations.length }} 位</a-tag>
      </template>
      <a-spin :spinning="loading">
        <div v-if="recommendations.length > 0" class="student-list">
          <a-card v-for="item in recommendations" :key="item.student.id" class="student-item" hoverable>
            <div class="student-header">
              <a-avatar :size="64" :src="item.student.avatarUrl">{{ item.student.name?.charAt(0) }}</a-avatar>
              <div class="student-basic">
                <h3>{{ item.student.name }}</h3>
                <p>{{ item.student.currentInstitution || '未知学校' }} · {{ item.student.major || '未知专业' }}</p>
                <a-tag :color="getDegreeColor(item.student.degreeLevel)">{{ item.student.degreeLevel || '未知学位' }}</a-tag>
              </div>
              <div class="match-score">
                <a-progress type="circle" :percent="Math.round((item.score || 0) * 100)" :width="60" :stroke-color="getScoreColor(item.score)" />
                <span class="score-label">匹配度</span>
              </div>
            </div>
            <a-divider />
            <div class="research-interests" v-if="item.student.researchInterests">
              <strong>研究兴趣：</strong>
              <a-tag v-for="interest in getInterests(item.student)" :key="interest" color="blue">{{ interest }}</a-tag>
            </div>
            <div class="recommendation-reason" v-if="item.reason">
              <InfoCircleOutlined /><span>{{ item.reason }}</span>
            </div>
            <div class="student-actions">
              <a-button type="primary" @click="startChat(item.student)"><MessageOutlined /> 发起聊天</a-button>
              <a-button @click="viewStudentDetail(item.student)"><EyeOutlined /> 查看详情</a-button>
            </div>
          </a-card>
        </div>
        <a-empty v-else-if="!loading" description="暂无推荐学生">
          <template #extra><p style="color: #999">完善您的招生需求可获取更精准的推荐</p></template>
        </a-empty>
      </a-spin>
    </a-card>

    <!-- 编辑招生需求弹窗 -->
    <a-modal v-model:visible="showEditModal" title="编辑招生需求" width="700px" @ok="handleSaveMentorInfo" :confirmLoading="saving">
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="研究方向（多个用逗号分隔）">
          <a-textarea v-model:value="editForm.researchAreas" placeholder="如：机器学习, 深度学习, 自然语言处理" :rows="2" />
        </a-form-item>
        <a-form-item label="研究关键词（多个用逗号分隔）">
          <a-input v-model:value="editForm.keywords" placeholder="如：NLP, LLM, Transformer, BERT" />
        </a-form-item>
        <a-form-item label="组内研究方向">
          <a-textarea v-model:value="editForm.groupDirection" :rows="3" placeholder="描述课题组当前的主要研究方向和项目" />
        </a-form-item>
        <a-form-item label="期望学生素质">
          <a-textarea v-model:value="editForm.expectedStudentQualities" :rows="2" placeholder="描述您期望学生具备的能力和素质" />
        </a-form-item>
        <a-form-item label="指导风格">
          <a-textarea v-model:value="editForm.mentoringStyle" :rows="2" placeholder="如：每周组会、一对一指导、鼓励自主探索等" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="可招收名额">
              <a-input-number v-model:value="editForm.availablePositions" :min="0" :max="20" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="是否接收学生">
              <a-switch v-model:checked="editForm.acceptingStudents" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="经费情况">
          <a-input v-model:value="editForm.fundingStatus" placeholder="如：国家自然科学基金、企业合作项目等" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 学生详情弹窗 -->
    <a-modal v-model:visible="showStudentModal" :title="selectedStudent?.name + ' - 学生详情'" width="800px" :footer="null">
      <div v-if="selectedStudent">
        <!-- 基本信息头部 -->
        <div class="student-detail-header">
          <a-avatar :size="80" :src="selectedStudent.avatar">{{ selectedStudent.name?.charAt(0) }}</a-avatar>
          <div class="student-detail-info">
            <h2>{{ selectedStudent.name }}</h2>
            <p>{{ selectedStudent.currentInstitution || '未知学校' }} · {{ selectedStudent.major || '未知专业' }}</p>
            <a-space>
              <a-tag :color="getDegreeColor(selectedStudent.degreeLevel)">{{ selectedStudent.degreeLevel || '未知学位' }}</a-tag>
              <a-tag v-if="selectedStudent.graduationYear" color="orange">{{ selectedStudent.graduationYear }}届</a-tag>
              <a-tag v-if="selectedStudent.gpa" color="green">GPA: {{ selectedStudent.gpa }}</a-tag>
              <a-tag v-if="selectedStudent.publicationsCount" color="purple">论文: {{ selectedStudent.publicationsCount }}篇</a-tag>
            </a-space>
          </div>
        </div>

        <a-divider />

        <!-- 研究兴趣 -->
        <div class="detail-section">
          <h4><ExperimentOutlined /> 研究兴趣</h4>
          <a-space wrap v-if="getInterests(selectedStudent).length">
            <a-tag v-for="i in getInterests(selectedStudent)" :key="i" color="blue">{{ i }}</a-tag>
          </a-space>
          <span v-else class="empty-text">未填写</span>
        </div>

        <!-- 研究关键词 -->
        <div class="detail-section" v-if="parseJson(selectedStudent.keywords).length">
          <h4><TagsOutlined /> 研究关键词</h4>
          <a-space wrap>
            <a-tag v-for="kw in parseJson(selectedStudent.keywords)" :key="kw" color="cyan">{{ kw }}</a-tag>
          </a-space>
        </div>

        <!-- 期望研究方向 -->
        <div class="detail-section" v-if="selectedStudent.expectedResearchDirection">
          <h4><AimOutlined /> 期望研究方向</h4>
          <p>{{ selectedStudent.expectedResearchDirection }}</p>
        </div>

        <a-divider />

        <!-- 技能与能力 -->
        <a-descriptions title="技能与能力" :column="1" bordered size="small">
          <a-descriptions-item label="个人能力">
            {{ selectedStudent.personalAbilities || '未填写' }}
          </a-descriptions-item>
          <a-descriptions-item label="编程技能">
            <a-space wrap v-if="getProgrammingSkills(selectedStudent).length">
              <a-tag v-for="s in getProgrammingSkills(selectedStudent)" :key="s" color="green">{{ s }}</a-tag>
            </a-space>
            <span v-else>未填写</span>
          </a-descriptions-item>
          <a-descriptions-item label="项目经验">
            <div style="white-space: pre-line">{{ selectedStudent.projectExperience || '未填写' }}</div>
          </a-descriptions-item>
        </a-descriptions>

        <a-divider />

        <!-- 其他信息 -->
        <a-descriptions title="其他信息" :column="2" bordered size="small">
          <a-descriptions-item label="邮箱">{{ selectedStudent.email || '未填写' }}</a-descriptions-item>
          <a-descriptions-item label="电话">{{ selectedStudent.phone || '未填写' }}</a-descriptions-item>
          <a-descriptions-item label="期望导师风格" :span="2">
            {{ selectedStudent.preferredMentorStyle || '未填写' }}
          </a-descriptions-item>
          <a-descriptions-item label="可用时间">{{ selectedStudent.availableTime || '未填写' }}</a-descriptions-item>
          <a-descriptions-item label="简历">
            <a v-if="selectedStudent.cvUrl" :href="selectedStudent.cvUrl" target="_blank">查看简历</a>
            <span v-else>未上传</span>
          </a-descriptions-item>
          <a-descriptions-item label="个人简介" :span="2">
            <div style="white-space: pre-line">{{ selectedStudent.bio || '未填写' }}</div>
          </a-descriptions-item>
        </a-descriptions>

        <div style="margin-top: 20px; text-align: right">
          <a-button type="primary" @click="startChat(selectedStudent)"><MessageOutlined /> 发起聊天</a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, computed, reactive, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, InfoCircleOutlined, ReloadOutlined, MessageOutlined, EyeOutlined, EditOutlined, BulbOutlined, ExperimentOutlined, TagsOutlined, AimOutlined } from '@ant-design/icons-vue'
import recommendationService from '../../service/recommendationService'
import mentorService from '../../service/mentorService'

export default defineComponent({
  name: 'RecommendedStudents',
  components: { UserOutlined, InfoCircleOutlined, ReloadOutlined, MessageOutlined, EyeOutlined, EditOutlined, BulbOutlined, ExperimentOutlined, TagsOutlined, AimOutlined },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const loadingMentor = ref(false)
    const saving = ref(false)
    const recommendations = ref([])
    const mentorInfo = ref(null)
    const showEditModal = ref(false)
    const showStudentModal = ref(false)
    const selectedStudent = ref(null)

    const editForm = reactive({
      researchAreas: '',
      keywords: '',
      groupDirection: '',
      expectedStudentQualities: '',
      mentoringStyle: '',
      availablePositions: 0,
      acceptingStudents: true,
      fundingStatus: ''
    })

    const currentUser = computed(() => store.getters['auth/currentUser'])

    const fetchMentorInfo = async () => {
      if (!currentUser.value?.id) return null
      loadingMentor.value = true
      try {
        const response = await mentorService.getMentorByUserId(currentUser.value.id)
        if (response.code === 0 && response.data) {
          mentorInfo.value = response.data
          // 填充编辑表单
          editForm.researchAreas = parseJsonToString(response.data.researchAreas)
          editForm.keywords = parseJsonToString(response.data.keywords)
          editForm.groupDirection = response.data.groupDirection || ''
          editForm.expectedStudentQualities = response.data.expectedStudentQualities || ''
          editForm.mentoringStyle = response.data.mentoringStyle || ''
          editForm.availablePositions = response.data.availablePositions || 0
          editForm.acceptingStudents = response.data.acceptingStudents !== false
          editForm.fundingStatus = response.data.fundingStatus || ''
          return response.data.id
        }
      } catch (error) {
        console.error('获取导师信息失败:', error)
      } finally {
        loadingMentor.value = false
      }
      return null
    }

    const handleSaveMentorInfo = async () => {
      if (!mentorInfo.value?.id) {
        message.warning('请先创建导师档案')
        return
      }
      saving.value = true
      try {
        const updateData = {
          researchAreas: stringToJsonArray(editForm.researchAreas),
          keywords: stringToJsonArray(editForm.keywords),
          groupDirection: editForm.groupDirection,
          expectedStudentQualities: editForm.expectedStudentQualities,
          mentoringStyle: editForm.mentoringStyle,
          availablePositions: editForm.availablePositions,
          acceptingStudents: editForm.acceptingStudents,
          fundingStatus: editForm.fundingStatus
        }
        const response = await mentorService.updateMentor(mentorInfo.value.id, updateData)
        if (response.code === 0) {
          message.success('招生需求保存成功')
          showEditModal.value = false
          await fetchMentorInfo()
          await fetchRecommendations()
        } else {
          message.error(response.message || '保存失败')
        }
      } catch (error) {
        console.error('保存失败:', error)
        message.error('保存失败')
      } finally {
        saving.value = false
      }
    }

    const fetchRecommendations = async () => {
      let mentorId = mentorInfo.value?.id
      if (!mentorId) { mentorId = await fetchMentorInfo() }
      if (!mentorId) { message.warning('请先完善导师信息'); return }
      loading.value = true
      try {
        const response = await recommendationService.getStudentRecommendations(mentorId, 10)
        if (response.code === 0) { recommendations.value = response.data || [] }
        else { message.warning(response.message || '获取推荐失败') }
      } catch (error) {
        console.error('获取推荐失败:', error)
        message.error('获取推荐失败')
      } finally {
        loading.value = false
      }
    }

    const parseJson = (value) => {
      if (!value) return []
      if (Array.isArray(value)) return value
      try { const p = JSON.parse(value); return Array.isArray(p) ? p : [] }
      catch { return typeof value === 'string' ? value.split(',').map(s => s.trim()).filter(s => s) : [] }
    }
    const parseJsonToString = (value) => parseJson(value).join(', ')
    const stringToJsonArray = (str) => { if (!str) return JSON.stringify([]); return JSON.stringify(str.split(',').map(s => s.trim()).filter(s => s)) }
    const getInterests = (student) => parseJson(student?.researchInterests).slice(0, 5)
    const getProgrammingSkills = (student) => parseJson(student?.programmingSkills).slice(0, 5)
    const getScoreColor = (score) => { if (!score) return '#1890ff'; if (score >= 0.8) return '#52c41a'; if (score >= 0.6) return '#1890ff'; if (score >= 0.4) return '#faad14'; return '#ff4d4f' }
    const getDegreeColor = (degree) => { if (!degree) return 'default'; if (degree.includes('PhD') || degree.includes('博士')) return 'red'; if (degree.includes('Master') || degree.includes('硕士')) return 'blue'; return 'green' }
    const startChat = (student) => { if (mentorInfo.value?.id) { router.push(`/chat/direct/${student.id}/${mentorInfo.value.id}`) } else { message.warning('请先完善导师信息') } }
    const viewStudentDetail = (student) => { selectedStudent.value = student; showStudentModal.value = true }

    onMounted(async () => { await fetchMentorInfo(); if (mentorInfo.value) { fetchRecommendations() } })

    return { loading, loadingMentor, saving, recommendations, mentorInfo, showEditModal, showStudentModal, selectedStudent, editForm,
      fetchRecommendations, handleSaveMentorInfo, parseJson, getInterests, getProgrammingSkills, getScoreColor, getDegreeColor, startChat, viewStudentDetail }
  }
})
</script>

<style scoped>
.recommendations-container { max-width: 1200px; margin: 0 auto; padding: 24px; }
.preferences-card { margin-bottom: 24px; }
.empty-text { color: #999; font-style: italic; }
.empty-text a { color: #1890ff; margin-left: 4px; }
.recommendations-card { margin-bottom: 24px; }
.student-list { display: flex; flex-direction: column; gap: 16px; }
.student-item { transition: all 0.3s; }
.student-item:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
.student-header { display: flex; align-items: center; gap: 16px; }
.student-basic { flex: 1; }
.student-basic h3 { margin: 0 0 4px 0; font-size: 18px; }
.student-basic p { margin: 0 0 8px 0; color: #666; }
.match-score { text-align: center; }
.score-label { display: block; margin-top: 4px; font-size: 12px; color: #999; }
.research-interests { margin: 12px 0; }
.recommendation-reason { margin: 12px 0; padding: 12px; background: linear-gradient(135deg, #fff7e6 0%, #fffbe6 100%); border-radius: 8px; font-size: 13px; color: #fa8c16; display: flex; align-items: flex-start; gap: 8px; line-height: 1.5; }
.student-actions { margin-top: 16px; display: flex; gap: 12px; }
/* 学生详情弹窗样式 */
.student-detail-header { display: flex; gap: 20px; align-items: center; margin-bottom: 16px; }
.student-detail-info { flex: 1; }
.student-detail-info h2 { margin: 0 0 8px 0; font-size: 24px; }
.student-detail-info p { margin: 0 0 12px 0; color: #666; font-size: 14px; }
.detail-section { margin: 16px 0; }
.detail-section h4 { margin-bottom: 8px; color: #1890ff; font-size: 14px; }
.detail-section p { margin: 0; color: #333; line-height: 1.6; }
@media screen and (max-width: 768px) { .recommendations-container { padding: 12px; } .student-header { flex-direction: column; text-align: center; } .student-actions { flex-direction: column; } .student-detail-header { flex-direction: column; text-align: center; } }
</style>
