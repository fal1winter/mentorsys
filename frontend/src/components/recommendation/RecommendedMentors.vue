<template>
  <div class="recommendations-container">
    <a-page-header
      title="个性化推荐"
      sub-title="基于您的研究偏好和个人能力为您推荐导师"
      @back="() => $router.push('/mentors')"
    >
      <template #extra>
        <a-space>
          <a-button @click="showEditModal = true" type="default">
            <EditOutlined />
            编辑偏好
          </a-button>
          <a-button @click="handleAnalyze" :loading="analyzing" type="primary">
            <SyncOutlined :spin="analyzing" />
            智能分析
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <!-- 快速偏好设置提示 -->
    <a-alert 
      v-if="!hasPreferences" 
      message="完善偏好获取更精准推荐" 
      description="填写您的研究兴趣、期望方向等信息，系统将为您匹配更合适的导师。您也可以让系统智能分析您的浏览历史自动生成偏好。"
      type="info" 
      show-icon 
      closable
      class="preference-alert"
    >
      <template #action>
        <a-button size="small" type="primary" @click="showEditModal = true">
          立即填写
        </a-button>
      </template>
    </a-alert>

    <!-- 完整偏好展示卡片 -->
    <a-card class="preferences-card">
      <template #title>
        <BulbOutlined /> 您的研究偏好
      </template>
      <template #extra>
        <a-button type="link" @click="showEditModal = true">
          <EditOutlined /> 编辑偏好
        </a-button>
      </template>
      <a-spin :spinning="preferencesLoading">
        <div v-if="userProfile && (userProfile.userPreferences || userProfile.systemAnalysis)">
          <!-- 用户填写的偏好 -->
          <a-descriptions title="个人偏好设置" :column="2" bordered size="small" v-if="userProfile.userPreferences">
            <a-descriptions-item label="研究兴趣" :span="2">
              <a-space wrap v-if="parseJson(userProfile.userPreferences.researchInterests).length">
                <a-tag v-for="item in parseJson(userProfile.userPreferences.researchInterests)" :key="item" color="blue">
                  {{ item }}
                </a-tag>
              </a-space>
              <span v-else class="empty-text">未填写 <a @click="showEditModal = true">点击填写</a></span>
            </a-descriptions-item>
            <a-descriptions-item label="期望研究方向" :span="2">
              <span v-if="userProfile.userPreferences.expectedResearchDirection">{{ userProfile.userPreferences.expectedResearchDirection }}</span>
              <span v-else class="empty-text">未填写 <a @click="showEditModal = true">点击填写</a></span>
            </a-descriptions-item>
            <a-descriptions-item label="个人能力">
              <span v-if="userProfile.userPreferences.personalAbilities">{{ userProfile.userPreferences.personalAbilities }}</span>
              <span v-else class="empty-text">未填写</span>
            </a-descriptions-item>
            <a-descriptions-item label="期望导师风格">
              <span v-if="userProfile.userPreferences.preferredMentorStyle">{{ userProfile.userPreferences.preferredMentorStyle }}</span>
              <span v-else class="empty-text">未填写</span>
            </a-descriptions-item>
            <a-descriptions-item label="编程技能" :span="2">
              <a-space wrap v-if="parseJson(userProfile.userPreferences.programmingSkills).length">
                <a-tag v-for="skill in parseJson(userProfile.userPreferences.programmingSkills)" :key="skill" color="green">
                  {{ skill }}
                </a-tag>
              </a-space>
              <span v-else class="empty-text">未填写</span>
            </a-descriptions-item>
            <a-descriptions-item label="项目经验" :span="2">
              <span v-if="userProfile.userPreferences.projectExperience">{{ userProfile.userPreferences.projectExperience }}</span>
              <span v-else class="empty-text">未填写</span>
            </a-descriptions-item>
          </a-descriptions>

          <!-- 系统分析的偏好 -->
          <a-divider v-if="userProfile.systemAnalysis" />
          <div v-if="userProfile.systemAnalysis" class="system-analysis">
            <h4><RobotOutlined /> 系统智能分析</h4>
            <p><strong>偏好总结：</strong>{{ userProfile.systemAnalysis.summary }}</p>
            <div class="tags-section" v-if="parseJson(userProfile.systemAnalysis.keywords).length">
              <strong>关键词：</strong>
              <a-space wrap>
                <a-tag v-for="keyword in parseJson(userProfile.systemAnalysis.keywords)" :key="keyword" color="purple">
                  {{ keyword }}
                </a-tag>
              </a-space>
            </div>
            <div class="tags-section" v-if="parseJson(userProfile.systemAnalysis.topics).length">
              <strong>研究主题：</strong>
              <a-space wrap>
                <a-tag v-for="topic in parseJson(userProfile.systemAnalysis.topics)" :key="topic" color="cyan">
                  {{ topic }}
                </a-tag>
              </a-space>
            </div>
            <p class="update-time">
              分析次数: {{ userProfile.systemAnalysis.analysisCount || 0 }} | 
              更新时间: {{ formatDate(userProfile.systemAnalysis.updateTime) }}
            </p>
          </div>
        </div>
        <a-empty v-else description="暂无偏好信息，请填写您的研究偏好以获取个性化推荐">
          <template #extra>
            <a-space>
              <a-button type="primary" @click="showEditModal = true">
                <EditOutlined /> 手动填写偏好
              </a-button>
              <a-button @click="handleAnalyze" :loading="analyzing">
                <RobotOutlined /> 智能分析生成
              </a-button>
            </a-space>
          </template>
        </a-empty>
      </a-spin>
    </a-card>

    <!-- 推荐导师列表 -->
    <a-card class="recommendations-card">
      <template #title>
        <span><StarOutlined /> 推荐导师</span>
        <a-tag color="purple" style="margin-left: 8px">{{ recommendations.length }} 位</a-tag>
        <a-tag v-if="recommendationType === 'fallback'" color="orange" style="margin-left: 4px">热门推荐</a-tag>
        <a-tag v-else-if="recommendationType === 'keyword'" color="blue" style="margin-left: 4px">关键词匹配</a-tag>
      </template>
      <template #extra>
        <a-button type="link" @click="fetchRecommendations" :loading="loading">
          <ReloadOutlined /> 刷新推荐
        </a-button>
      </template>
      <a-spin :spinning="loading">
        <div v-if="recommendations.length > 0" class="mentor-grid">
          <div
            v-for="item in recommendations"
            :key="item.mentor.id"
            class="recommendation-item"
          >
            <mentor-card :mentor="item.mentor" />
            <div class="match-details" v-if="item.matchDetails">
              <a-progress 
                :percent="Math.round((item.score || 0) * 100)" 
                :stroke-color="getScoreColor(item.score)"
                size="small"
              />
              <div class="detail-tags">
                <a-tooltip v-for="(value, key) in item.matchDetails" :key="key" :title="getDimensionLabel(key)">
                  <a-tag :color="getDimensionColor(key)">
                    {{ getDimensionLabel(key) }} {{ formatPercent(value) }}
                  </a-tag>
                </a-tooltip>
              </div>
            </div>
            <div class="recommendation-reason" v-if="item.reason">
              <InfoCircleOutlined />
              <span>{{ item.reason }}</span>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无推荐结果">
          <template #extra>
            <a-space direction="vertical" align="center">
              <p style="color: #666">完善偏好信息可获取更精准的推荐</p>
              <a-space>
                <a-button type="primary" @click="showEditModal = true">
                  填写偏好
                </a-button>
                <router-link to="/mentors">
                  <a-button>浏览全部导师</a-button>
                </router-link>
              </a-space>
            </a-space>
          </template>
        </a-empty>
      </a-spin>
    </a-card>

    <!-- 编辑偏好弹窗 -->
    <a-modal
      v-model:visible="showEditModal"
      title="编辑研究偏好"
      width="700px"
      @ok="handleSavePreferences"
      :confirmLoading="saving"
    >
      <a-form :model="editForm" layout="vertical">
        <a-form-item label="研究兴趣（多个用逗号分隔）">
          <a-input v-model:value="editForm.researchInterests" placeholder="如：机器学习, 深度学习, 自然语言处理" />
        </a-form-item>
        <a-form-item label="期望研究方向">
          <a-textarea v-model:value="editForm.expectedResearchDirection" :rows="2" 
            placeholder="描述您期望的具体研究方向，如：希望从事大语言模型的研究，特别是模型压缩和推理优化方向" />
        </a-form-item>
        <a-form-item label="个人能力描述">
          <a-textarea v-model:value="editForm.personalAbilities" :rows="2" 
            placeholder="描述您的学术能力和技术特长，如：熟悉深度学习框架，有论文发表经验" />
        </a-form-item>
        <a-form-item label="期望导师风格">
          <a-input v-model:value="editForm.preferredMentorStyle" 
            placeholder="如：希望导师能够给予充分的研究自由度，定期进行学术讨论" />
        </a-form-item>
        <a-form-item label="编程技能（多个用逗号分隔）">
          <a-input v-model:value="editForm.programmingSkills" placeholder="如：Python, PyTorch, TensorFlow, C++" />
        </a-form-item>
        <a-form-item label="项目经验">
          <a-textarea v-model:value="editForm.projectExperience" :rows="3" 
            placeholder="描述您参与过的研究项目或实习经历" />
        </a-form-item>
        <a-form-item label="可用时间">
          <a-input v-model:value="editForm.availableTime" placeholder="如：全日制、每周20小时" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted, reactive } from 'vue'
import { useStore } from 'vuex'
import { message } from 'ant-design-vue'
import { 
  BulbOutlined, 
  InfoCircleOutlined, 
  StarOutlined, 
  SyncOutlined, 
  ReloadOutlined,
  EditOutlined,
  RobotOutlined
} from '@ant-design/icons-vue'
import MentorCard from '../mentor/MentorCard.vue'
import recommendationService from '../../service/recommendationService'
import studentService from '../../service/studentService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'RecommendedMentors',
  components: {
    BulbOutlined,
    InfoCircleOutlined,
    StarOutlined,
    SyncOutlined,
    ReloadOutlined,
    EditOutlined,
    RobotOutlined,
    MentorCard
  },
  setup() {
    const store = useStore()
    const loading = ref(false)
    const preferencesLoading = ref(false)
    const analyzing = ref(false)
    const saving = ref(false)
    const recommendations = ref([])
    const userProfile = ref(null)
    const showEditModal = ref(false)
    const recommendationType = ref('') // 'semantic', 'keyword', 'fallback'

    const editForm = reactive({
      researchInterests: '',
      expectedResearchDirection: '',
      personalAbilities: '',
      preferredMentorStyle: '',
      programmingSkills: '',
      projectExperience: '',
      availableTime: ''
    })

    const currentUser = computed(() => store.getters['auth/currentUser'])
    
    // 检查是否有偏好信息
    const hasPreferences = computed(() => {
      if (!userProfile.value) return false
      const prefs = userProfile.value.userPreferences
      const sys = userProfile.value.systemAnalysis
      
      // 检查用户填写的偏好
      if (prefs) {
        if (prefs.researchInterests && parseJson(prefs.researchInterests).length > 0) return true
        if (prefs.expectedResearchDirection) return true
        if (prefs.personalAbilities) return true
        if (prefs.programmingSkills && parseJson(prefs.programmingSkills).length > 0) return true
      }
      
      // 检查系统分析的偏好
      if (sys && sys.summary) return true
      
      return false
    })

    const fetchRecommendations = async () => {
      if (!currentUser.value?.id) {
        message.warning('请先登录')
        return
      }
      
      loading.value = true
      recommendationType.value = ''
      try {
        const response = await recommendationService.getMentorRecommendations(currentUser.value.id, 10)
        if (response.code === 0) {
          recommendations.value = response.data || []
          
          // 判断推荐类型
          if (recommendations.value.length > 0) {
            const firstMatch = recommendations.value[0].matchDetails || {}
            if (firstMatch.popularity !== undefined) {
              recommendationType.value = 'fallback'
            } else if (firstMatch.keyword_match !== undefined) {
              recommendationType.value = 'keyword'
            } else {
              recommendationType.value = 'semantic'
            }
          }
        } else {
          message.warning(response.message || '获取推荐失败')
        }
      } catch (error) {
        console.error('获取推荐失败:', error)
        message.error('获取推荐失败')
      } finally {
        loading.value = false
      }
    }

    const fetchUserProfile = async () => {
      if (!currentUser.value?.id) return
      
      preferencesLoading.value = true
      try {
        const response = await recommendationService.getUserProfile(currentUser.value.id)
        if (response.code === 0) {
          userProfile.value = response.data
          // 填充编辑表单
          if (response.data?.userPreferences) {
            const prefs = response.data.userPreferences
            editForm.researchInterests = parseJsonToString(prefs.researchInterests)
            editForm.expectedResearchDirection = prefs.expectedResearchDirection || ''
            editForm.personalAbilities = prefs.personalAbilities || ''
            editForm.preferredMentorStyle = prefs.preferredMentorStyle || ''
            editForm.programmingSkills = parseJsonToString(prefs.programmingSkills)
            editForm.projectExperience = prefs.projectExperience || ''
            editForm.availableTime = prefs.availableTime || ''
          }
        }
      } catch (error) {
        console.error('获取用户档案失败:', error)
      } finally {
        preferencesLoading.value = false
      }
    }

    const handleAnalyze = async () => {
      if (!currentUser.value?.id) {
        message.warning('请先登录')
        return
      }
      
      analyzing.value = true
      try {
        const response = await recommendationService.analyzePreferences(currentUser.value.id)
        if (response.code === 0) {
          message.success('偏好分析完成')
          await fetchUserProfile()
          await fetchRecommendations()
        } else {
          message.warning(response.message || '分析失败')
        }
      } catch (error) {
        console.error('分析失败:', error)
        message.error('分析失败')
      } finally {
        analyzing.value = false
      }
    }

    const handleSavePreferences = async () => {
      if (!currentUser.value?.id) {
        message.warning('请先登录')
        return
      }

      saving.value = true
      try {
        // 构建更新数据
        const updateData = {
          researchInterests: stringToJsonArray(editForm.researchInterests),
          expectedResearchDirection: editForm.expectedResearchDirection,
          personalAbilities: editForm.personalAbilities,
          preferredMentorStyle: editForm.preferredMentorStyle,
          programmingSkills: stringToJsonArray(editForm.programmingSkills),
          projectExperience: editForm.projectExperience,
          availableTime: editForm.availableTime
        }

        const response = await studentService.updateStudent(currentUser.value.id, updateData)
        if (response.code === 0) {
          message.success('偏好保存成功')
          showEditModal.value = false
          // 清除推荐缓存并重新获取
          await recommendationService.invalidateCache(currentUser.value.id, null)
          await fetchUserProfile()
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

    const parseJson = (value) => {
      if (!value) return []
      if (Array.isArray(value)) return value
      try {
        const parsed = JSON.parse(value)
        return Array.isArray(parsed) ? parsed : []
      } catch {
        if (typeof value === 'string') {
          return value.split(',').map(s => s.trim()).filter(s => s)
        }
        return []
      }
    }

    const parseJsonToString = (value) => {
      const arr = parseJson(value)
      return arr.join(', ')
    }

    const stringToJsonArray = (str) => {
      if (!str) return JSON.stringify([])
      const arr = str.split(',').map(s => s.trim()).filter(s => s)
      return JSON.stringify(arr)
    }

    const formatDate = (date) => {
      if (!date) return '未知'
      return dayjs(date).format('YYYY-MM-DD HH:mm')
    }

    const formatPercent = (value) => {
      if (value === undefined || value === null) return '0%'
      return Math.round(value * 100) + '%'
    }

    const getScoreColor = (score) => {
      if (!score) return '#1890ff'
      if (score >= 0.8) return '#52c41a'
      if (score >= 0.6) return '#1890ff'
      if (score >= 0.4) return '#faad14'
      return '#ff4d4f'
    }

    const getDimensionLabel = (key) => {
      const labels = {
        'research_interests': '研究兴趣',
        'expected_direction': '期望方向',
        'personal_abilities': '个人能力',
        'programming_skills': '编程技能',
        'llm_preference': '智能分析',
        'researchMatch': '研究匹配',
        'qualityScore': '导师质量',
        'availabilityScore': '接收能力',
        'workloadScore': '工作强度',
        'keyword_match': '关键词匹配',
        'quality_bonus': '质量加分',
        'student_bonus': '学生素质',
        'popularity': '热门程度',
        'default': '综合匹配'
      }
      return labels[key] || key
    }

    const getDimensionColor = (key) => {
      const colors = {
        'research_interests': 'blue',
        'expected_direction': 'green',
        'personal_abilities': 'orange',
        'programming_skills': 'purple',
        'llm_preference': 'cyan',
        'researchMatch': 'blue',
        'qualityScore': 'green',
        'availabilityScore': 'orange',
        'workloadScore': 'purple',
        'keyword_match': 'geekblue',
        'quality_bonus': 'gold',
        'student_bonus': 'lime',
        'popularity': 'magenta'
      }
      return colors[key] || 'default'
    }

    onMounted(() => {
      fetchUserProfile()
      fetchRecommendations()
    })

    return {
      loading,
      preferencesLoading,
      analyzing,
      saving,
      recommendations,
      userProfile,
      showEditModal,
      editForm,
      hasPreferences,
      recommendationType,
      fetchRecommendations,
      fetchUserProfile,
      handleAnalyze,
      handleSavePreferences,
      parseJson,
      formatDate,
      formatPercent,
      getScoreColor,
      getDimensionLabel,
      getDimensionColor
    }
  }
})
</script>

<style scoped>
.recommendations-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.preference-alert {
  margin-bottom: 16px;
}

.preferences-card {
  margin-bottom: 24px;
}

.empty-text {
  color: #999;
  font-style: italic;
}

.empty-text a {
  color: #1890ff;
  margin-left: 4px;
}

.system-analysis {
  margin-top: 16px;
  padding: 16px;
  background: linear-gradient(135deg, #f0f5ff 0%, #e6f7ff 100%);
  border-radius: 8px;
}

.system-analysis h4 {
  margin-bottom: 12px;
  color: #1890ff;
}

.tags-section {
  margin: 12px 0;
}

.update-time {
  margin-top: 12px;
  color: #999;
  font-size: 12px;
}

.recommendations-card {
  margin-bottom: 24px;
}

.mentor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
}

.recommendation-item {
  position: relative;
}

.match-details {
  margin-top: 12px;
  padding: 12px;
  background: #fafafa;
  border-radius: 8px;
}

.detail-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.recommendation-reason {
  margin-top: 8px;
  padding: 12px;
  background: linear-gradient(135deg, #e6f7ff 0%, #f0f5ff 100%);
  border-radius: 8px;
  font-size: 13px;
  color: #1890ff;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  line-height: 1.5;
}

.recommendation-reason .anticon {
  flex-shrink: 0;
  margin-top: 2px;
}

/* 响应式布局 */
@media screen and (max-width: 768px) {
  .recommendations-container {
    padding: 12px;
  }
  
  .mentor-grid {
    grid-template-columns: 1fr;
  }
  
  .detail-tags {
    flex-direction: column;
  }
}
</style>
