<template>
  <div class="list-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2><UserOutlined /> 学生列表</h2>
        <p>浏览所有注册学生，查看他们的研究兴趣和背景</p>
      </div>
      <div class="header-stats">
        <a-statistic title="学生总数" :value="pagination.total" />
        <a-statistic title="博士生" :value="phdCount" suffix="人" />
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <a-row :gutter="[16, 16]" align="middle">
        <a-col :xs="24" :sm="24" :md="8" :lg="5">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索学生姓名、学校、研究方向..."
            size="large"
            @search="handleSearch"
            allowClear
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.institution"
            :options="institutionOptions"
            placeholder="学校"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onInstitutionSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.major"
            :options="majorOptions"
            placeholder="专业"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onMajorSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-select
            v-model:value="filters.degreeLevel"
            placeholder="学位类型"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
          >
            <a-select-option value="本科">本科</a-select-option>
            <a-select-option value="硕士">硕士</a-select-option>
            <a-select-option value="博士">博士</a-select-option>
          </a-select>
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.researchInterest"
            :options="researchAreaOptions"
            placeholder="研究方向"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onResearchAreaSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.skill"
            :options="skillOptions"
            placeholder="编程技能"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onSkillSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="4">
          <a-button @click="resetFilters">重置筛选</a-button>
        </a-col>
      </a-row>
    </div>

    <!-- 学生列表 -->
    <a-spin :spinning="loading">
      <div v-if="students.length > 0" class="card-grid">
        <div v-for="student in students" :key="student.id" class="card-item" @click="viewDetail(student)">
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
            <a-button type="link" size="small" @click.stop="showDetailModal(student, $event)">
              <EyeOutlined /> 快速预览
            </a-button>
            <a-button 
              v-if="isMentor" 
              type="link" 
              size="small"
              @click.stop="startChat(student)"
            >
              <MessageOutlined /> 联系
            </a-button>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无学生数据" />
    </a-spin>

    <!-- 分页 -->
    <div class="pagination">
      <a-pagination
        v-model:current="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :show-total="total => `共 ${total} 位学生`"
        show-size-changer
        :page-size-options="['12', '24', '36']"
        @change="handlePageChange"
      />
    </div>

    <!-- 学生详情弹窗 -->
    <a-modal
      v-model:visible="detailVisible"
      :title="selectedStudent?.name + ' - 详细信息'"
      width="700px"
      :footer="null"
    >
      <student-detail-modal :student="selectedStudent" />
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  UserOutlined, TrophyOutlined, CalendarOutlined, FileTextOutlined,
  EyeOutlined, MessageOutlined 
} from '@ant-design/icons-vue'
import StudentDetailModal from './StudentDetailModal.vue'
import studentService from '@/service/studentService'

export default defineComponent({
  name: 'StudentList',
  components: {
    UserOutlined, TrophyOutlined, CalendarOutlined, FileTextOutlined,
    EyeOutlined, MessageOutlined,
    StudentDetailModal
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const searchKeyword = ref('')
    const students = ref([])
    const detailVisible = ref(false)
    const selectedStudent = ref(null)
    const phdCount = ref(0)

    const filters = reactive({
      institution: '',
      major: '',
      degreeLevel: undefined,
      researchInterest: '',
      skill: ''
    })

    const pagination = reactive({
      current: 1,
      pageSize: 12,
      total: 0
    })

    // 预设选项
    const defaultInstitutions = [
      '清华大学', '北京大学', '浙江大学', '复旦大学', 
      '上海交通大学', '南京大学', '中国科学技术大学', '哈尔滨工业大学',
      '中国科学院', '西安交通大学', '华中科技大学', '武汉大学'
    ]

    const defaultMajors = [
      '计算机科学与技术', '软件工程', '人工智能', '电子信息',
      '数据科学', '信息与计算科学', '自动化', '通信工程'
    ]

    const defaultResearchAreas = [
      '人工智能', '机器学习', '深度学习', '计算机视觉', 
      '自然语言处理', '推荐系统', '强化学习', '知识图谱',
      '数据挖掘', '图神经网络', '多模态学习', '大语言模型'
    ]

    const defaultSkills = [
      'Python', 'Java', 'C++', 'JavaScript', 'PyTorch', 'TensorFlow',
      'SQL', 'Linux', 'Git', 'Docker', 'Spark', 'CUDA'
    ]

    // 动态选项
    const institutionOptions = ref(defaultInstitutions.map(v => ({ value: v })))
    const majorOptions = ref(defaultMajors.map(v => ({ value: v })))
    const researchAreaOptions = ref(defaultResearchAreas.map(v => ({ value: v })))
    const skillOptions = ref(defaultSkills.map(v => ({ value: v })))

    const onInstitutionSearch = (searchText) => {
      institutionOptions.value = defaultInstitutions
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !institutionOptions.value.find(o => o.value === searchText)) {
        institutionOptions.value.unshift({ value: searchText })
      }
    }

    const onMajorSearch = (searchText) => {
      majorOptions.value = defaultMajors
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !majorOptions.value.find(o => o.value === searchText)) {
        majorOptions.value.unshift({ value: searchText })
      }
    }

    const onResearchAreaSearch = (searchText) => {
      researchAreaOptions.value = defaultResearchAreas
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !researchAreaOptions.value.find(o => o.value === searchText)) {
        researchAreaOptions.value.unshift({ value: searchText })
      }
    }

    const onSkillSearch = (searchText) => {
      skillOptions.value = defaultSkills
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !skillOptions.value.find(o => o.value === searchText)) {
        skillOptions.value.unshift({ value: searchText })
      }
    }

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

    const fetchStudents = async () => {
      loading.value = true
      try {
        const response = await studentService.getStudentList({
          page: pagination.current,
          limit: pagination.pageSize,
          keyword: searchKeyword.value || undefined,
          institution: filters.institution || undefined,
          major: filters.major || undefined,
          degreeLevel: filters.degreeLevel || undefined,
          researchInterest: filters.researchInterest || undefined,
          skill: filters.skill || undefined
        })
        if (response.code === 0) {
          students.value = response.data || []
          pagination.total = response.total || 0
          phdCount.value = (response.data || []).filter(s => s.degreeLevel === '博士').length
        }
      } catch (error) {
        message.error('获取学生列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      pagination.current = 1
      fetchStudents()
    }

    const handleFilterChange = () => {
      pagination.current = 1
      fetchStudents()
    }

    const handlePageChange = (page, pageSize) => {
      pagination.current = page
      pagination.pageSize = pageSize
      fetchStudents()
    }

    const resetFilters = () => {
      searchKeyword.value = ''
      filters.institution = ''
      filters.major = ''
      filters.degreeLevel = undefined
      filters.researchInterest = ''
      filters.skill = ''
      pagination.current = 1
      fetchStudents()
    }

    const parseJsonArray = (jsonStr) => {
      if (!jsonStr) return []
      try {
        const parsed = JSON.parse(jsonStr)
        return Array.isArray(parsed) ? parsed : []
      } catch {
        return []
      }
    }

    const viewDetail = (student) => {
      // 跳转到学生详情页
      router.push(`/students/${student.id}`)
    }

    const showDetailModal = (student, event) => {
      // 阻止冒泡，防止触发卡片点击
      event.stopPropagation()
      selectedStudent.value = student
      detailVisible.value = true
    }

    const startChat = (student) => {
      if (!currentUser.value) {
        message.warning('请先登录')
        return
      }
      router.push(`/chat/direct/${student.id}/${currentUser.value.id}`)
    }

    onMounted(() => {
      fetchStudents()
    })

    return {
      loading, searchKeyword, students, filters, pagination,
      isMentor, detailVisible, selectedStudent, phdCount, getGradient,
      institutionOptions, majorOptions, researchAreaOptions, skillOptions,
      onInstitutionSearch, onMajorSearch, onResearchAreaSearch, onSkillSearch,
      handleSearch, handleFilterChange, handlePageChange, resetFilters,
      parseJsonArray, viewDetail, showDetailModal, startChat
    }
  }
})
</script>

<style scoped>
.list-container {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
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

.header-stats {
  display: flex;
  gap: 32px;
}

.header-stats :deep(.ant-statistic-title) {
  color: rgba(255, 255, 255, 0.85);
}

.header-stats :deep(.ant-statistic-content) {
  color: #fff;
}

.search-section {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 24px;
}

.card-item {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.card-item:hover {
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
</style>
