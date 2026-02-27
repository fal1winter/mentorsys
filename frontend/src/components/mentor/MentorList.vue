<template>
  <div class="list-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2><TeamOutlined /> 导师列表</h2>
        <p>浏览所有导师，了解他们的研究方向和招生情况</p>
      </div>
      <div class="header-stats">
        <a-statistic title="导师总数" :value="pagination.total" />
        <a-statistic title="接收学生" :value="acceptingCount" suffix="人" />
      </div>
    </div>

    <!-- 搜索和筛选区域 -->
    <div class="search-section">
      <a-row :gutter="[16, 16]" align="middle">
        <a-col :xs="24" :sm="24" :md="8" :lg="6">
          <a-input-search
            v-model:value="searchKeyword"
            placeholder="搜索导师姓名、研究方向..."
            size="large"
            @search="handleSearch"
            allowClear
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.institution"
            :options="institutionOptions"
            placeholder="学校/机构"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onInstitutionSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.researchArea"
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
            v-model:value="filters.title"
            :options="titleOptions"
            placeholder="职称"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onTitleSearch"
          />
        </a-col>
        <a-col :xs="12" :sm="8" :md="4" :lg="3">
          <a-auto-complete
            v-model:value="filters.department"
            :options="departmentOptions"
            placeholder="院系"
            style="width: 100%"
            allowClear
            @change="handleFilterChange"
            @search="onDepartmentSearch"
          />
        </a-col>
        <a-col :xs="24" :sm="12" :md="24" :lg="6">
          <a-space wrap>
            <a-checkbox v-model:checked="filters.acceptingOnly" @change="handleFilterChange">
              仅显示接收学生
            </a-checkbox>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-col>
      </a-row>
    </div>

    <!-- 导师列表 -->
    <a-spin :spinning="loading">
      <div v-if="mentors.length > 0" class="card-grid">
        <div v-for="mentor in mentors" :key="mentor.id" class="card-item" @click="goToDetail(mentor.id)">
          <div class="card-cover" :style="{ background: getGradient(mentor.id) }">
            <a-avatar :size="72" :src="mentor.avatarUrl" class="card-avatar">
              {{ mentor.name?.charAt(0) }}
            </a-avatar>
            <div class="card-status" :class="mentor.acceptingStudents ? 'accepting' : 'not-accepting'">
              {{ mentor.acceptingStudents ? '接收学生' : '暂不接收' }}
            </div>
          </div>
          <div class="card-body">
            <h3 class="card-title">{{ mentor.name }}</h3>
            <p class="card-subtitle">{{ mentor.title }} · {{ mentor.institution }}</p>
            
            <div class="card-info">
              <div class="info-row">
                <ExperimentOutlined />
                <span>{{ mentor.researchAreas || '未设置研究方向' }}</span>
              </div>
              <div class="info-row" v-if="mentor.department">
                <BankOutlined />
                <span>{{ mentor.department }}</span>
              </div>
              <div class="info-row">
                <TeamOutlined />
                <span>{{ mentor.currentStudents || 0 }}/{{ mentor.maxStudents || 5 }} 学生</span>
              </div>
              <div class="info-row">
                <StarFilled class="star-icon" />
                <span>{{ mentor.ratingAvg ? Number(mentor.ratingAvg).toFixed(1) : '暂无' }} 评分</span>
              </div>
            </div>

            <div class="card-tags" v-if="mentor.keywords">
              <a-tag v-for="tag in parseKeywords(mentor.keywords).slice(0, 3)" :key="tag" color="blue">
                {{ tag }}
              </a-tag>
            </div>
          </div>
          <div class="card-footer">
            <span><EyeOutlined /> {{ mentor.viewCount || 0 }}</span>
            <a-button type="link" size="small">查看详情 <RightOutlined /></a-button>
          </div>
        </div>
      </div>
      <a-empty v-else description="暂无导师数据" />
    </a-spin>

    <!-- 分页 -->
    <div class="pagination">
      <a-pagination
        v-model:current="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :show-total="total => `共 ${total} 位导师`"
        show-size-changer
        :page-size-options="['12', '24', '36']"
        @change="handlePageChange"
      />
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  TeamOutlined, ExperimentOutlined, StarFilled, BankOutlined,
  EyeOutlined, RightOutlined 
} from '@ant-design/icons-vue'

export default defineComponent({
  name: 'MentorList',
  components: {
    TeamOutlined, ExperimentOutlined, StarFilled, BankOutlined,
    EyeOutlined, RightOutlined
  },
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const searchKeyword = ref('')
    const acceptingCount = ref(0)

    const filters = reactive({
      institution: '',
      researchArea: '',
      title: '',
      department: '',
      acceptingOnly: false
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

    const defaultResearchAreas = [
      '人工智能', '机器学习', '深度学习', '计算机视觉', 
      '自然语言处理', '推荐系统', '强化学习', '知识图谱',
      '数据挖掘', '图神经网络', '多模态学习', '大语言模型'
    ]

    const defaultTitles = ['教授', '副教授', '研究员', '副研究员', '讲师', '助理教授']

    const defaultDepartments = [
      '计算机科学与技术系', '人工智能学院', '软件学院', '信息学院',
      '电子工程系', '自动化系', '数学系'
    ]

    // 动态选项
    const institutionOptions = ref(defaultInstitutions.map(v => ({ value: v })))
    const researchAreaOptions = ref(defaultResearchAreas.map(v => ({ value: v })))
    const titleOptions = ref(defaultTitles.map(v => ({ value: v })))
    const departmentOptions = ref(defaultDepartments.map(v => ({ value: v })))

    const onInstitutionSearch = (searchText) => {
      institutionOptions.value = defaultInstitutions
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !institutionOptions.value.find(o => o.value === searchText)) {
        institutionOptions.value.unshift({ value: searchText })
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

    const onTitleSearch = (searchText) => {
      titleOptions.value = defaultTitles
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !titleOptions.value.find(o => o.value === searchText)) {
        titleOptions.value.unshift({ value: searchText })
      }
    }

    const onDepartmentSearch = (searchText) => {
      departmentOptions.value = defaultDepartments
        .filter(item => item.toLowerCase().includes(searchText.toLowerCase()))
        .map(v => ({ value: v }))
      if (searchText && !departmentOptions.value.find(o => o.value === searchText)) {
        departmentOptions.value.unshift({ value: searchText })
      }
    }

    const mentors = computed(() => store.getters['mentor/mentorList'])

    const gradients = [
      'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
      'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
      'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
      'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
      'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)'
    ]

    const getGradient = (id) => gradients[id % gradients.length]

    const parseKeywords = (keywords) => {
      if (!keywords) return []
      try {
        const parsed = JSON.parse(keywords)
        return Array.isArray(parsed) ? parsed : keywords.split(',').map(s => s.trim())
      } catch {
        return keywords.split(',').map(s => s.trim())
      }
    }

    const fetchMentors = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.current,
          limit: pagination.pageSize,
          keyword: searchKeyword.value || undefined,
          institution: filters.institution || undefined,
          researchArea: filters.researchArea || undefined,
          title: filters.title || undefined,
          department: filters.department || undefined,
          acceptingStudents: filters.acceptingOnly || undefined
        }

        const response = await store.dispatch('mentor/fetchMentors', params)
        if (response.code === 0) {
          pagination.total = response.total || 0
          acceptingCount.value = (response.data || []).filter(m => m.acceptingStudents).length
        }
      } catch (error) {
        message.error('获取导师列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      pagination.current = 1
      fetchMentors()
    }

    const handleFilterChange = () => {
      pagination.current = 1
      fetchMentors()
    }

    const handlePageChange = (page, pageSize) => {
      pagination.current = page
      pagination.pageSize = pageSize
      fetchMentors()
    }

    const resetFilters = () => {
      searchKeyword.value = ''
      filters.institution = ''
      filters.researchArea = ''
      filters.title = ''
      filters.department = ''
      filters.acceptingOnly = false
      pagination.current = 1
      fetchMentors()
    }

    const goToDetail = (id) => {
      router.push(`/mentors/${id}`)
    }

    onMounted(() => {
      fetchMentors()
    })

    return {
      loading, searchKeyword, filters, pagination, mentors,
      acceptingCount, getGradient, parseKeywords,
      institutionOptions, researchAreaOptions, titleOptions, departmentOptions,
      onInstitutionSearch, onResearchAreaSearch, onTitleSearch, onDepartmentSearch,
      handleSearch, handleFilterChange, handlePageChange, resetFilters, goToDetail
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

.card-status {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.card-status.accepting {
  background: rgba(82, 196, 26, 0.9);
  color: #fff;
}

.card-status.not-accepting {
  background: rgba(255, 77, 79, 0.9);
  color: #fff;
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

.star-icon {
  color: #faad14 !important;
}

.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #f0f0f0;
  background: #fafafa;
  font-size: 13px;
  color: #999;
}

.pagination {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>
