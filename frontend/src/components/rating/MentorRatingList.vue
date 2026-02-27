<template>
  <div class="mentor-rating-list">
    <div class="rating-header">
      <h3>学生评价 ({{ total }})</h3>
      <div class="sort-controls">
        <a-button-group>
          <a-button 
            :type="sortBy === 'createTime' ? 'primary' : 'default'" 
            @click="changeSortBy('createTime')">
            按时间
          </a-button>
          <a-button 
            :type="sortBy === 'rating' ? 'primary' : 'default'" 
            @click="changeSortBy('rating')">
            按评分
          </a-button>
        </a-button-group>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-empty v-if="ratings.length === 0" description="暂无评价" />
      <div v-else class="ratings-container">
        <div v-for="rating in ratings" :key="rating.id" class="rating-item">
          <div class="rating-header-row">
            <div class="rating-score">
              <span class="score-number">{{ rating.rating }}</span>
              <a-rate :value="rating.rating / 2" disabled allow-half />
            </div>
            <span class="rating-date">{{ formatDate(rating.createTime) }}</span>
          </div>
          <div class="rating-content" v-html="rating.comment"></div>
          <div class="rating-footer">
            <div class="rating-actions">
              <a-button
                v-if="isOwnRating(rating)"
                type="link"
                size="small"
                @click="handleEdit(rating)"
              >
                <EditOutlined /> 编辑
              </a-button>
              <a-popconfirm
                v-if="isOwnRating(rating)"
                title="确定要删除这条评价吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(rating)"
              >
                <a-button type="link" danger size="small">
                  <DeleteOutlined /> 删除
                </a-button>
              </a-popconfirm>
            </div>
            <div class="helpful-actions">
              <a-button
                type="link"
                size="small"
                :disabled="likedRatings.has(rating.id)"
                @click="handleLike(rating)"
              >
                <LikeFilled v-if="likedRatings.has(rating.id)" style="color: #1890ff;" />
                <LikeOutlined v-else />
                {{ rating.helpfulCount || 0 }}
              </a-button>
            </div>
          </div>
        </div>
      </div>
    </a-spin>

    <div class="pagination-container" v-if="total > 0">
      <a-pagination
        v-model:current="currentPage"
        v-model:pageSize="pageSize"
        :total="total"
        @change="handlePageChange"
        showSizeChanger
        :pageSizeOptions="['5', '10', '20']"
        :showTotal="(total) => `共 ${total} 条评价`"
      />
    </div>

    <!-- 编辑评价弹窗 -->
    <a-modal
      v-model:visible="editModalVisible"
      title="编辑评价"
      :confirmLoading="submitting"
      @ok="handleEditSubmit"
      @cancel="handleEditCancel"
      width="600px"
    >
      <div class="edit-form">
        <div class="form-item">
          <label class="form-label">评分 (0.1-10.0)</label>
          <div class="rating-input">
            <div class="rating-row">
              <span class="rating-label">整数部分 (0-5):</span>
              <a-rate
                :value="integerRating"
                @change="handleIntegerChange"
                :count="5"
              />
              <span class="rating-value">{{ integerRating * 2 }} 分</span>
            </div>
            <div class="rating-row">
              <span class="rating-label">小数部分 (0-0.9):</span>
              <a-rate
                :value="decimalRating"
                @change="handleDecimalChange"
                :count="5"
                allow-half
              />
              <span class="rating-value">{{ (decimalRating * 2).toFixed(1) }} 分</span>
            </div>
            <div class="rating-total">
              总分: <strong>{{ editForm.rating.toFixed(1) }}</strong> / 10.0
            </div>
          </div>
        </div>
        <div class="form-item">
          <label class="form-label">评价内容</label>
          <a-textarea
            v-model:value="editForm.comment"
            :rows="6"
            placeholder="请输入评价内容"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { LikeOutlined, LikeFilled, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import { useStore } from 'vuex'
import ratingService from '../../service/ratingService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'MentorRatingList',
  components: {
    LikeOutlined,
    LikeFilled,
    EditOutlined,
    DeleteOutlined
  },
  props: {
    mentorId: {
      type: Number,
      required: true
    }
  },
  setup(props) {
    const store = useStore()
    const loading = ref(false)
    const ratings = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const sortBy = ref('createTime')
    const likedRatings = ref(new Set())
    const editModalVisible = ref(false)
    const editingRating = ref(null)
    const submitting = ref(false)
    const integerRating = ref(0)
    const decimalRating = ref(0)

    const editForm = reactive({
      rating: 0,
      comment: ''
    })

    const currentUser = computed(() => store.getters['auth/currentUser'])
    const isAuthenticated = computed(() => store.getters['auth/isAuthenticated'])
    const userRole = computed(() => store.getters['auth/userRole'])

    const fetchRatings = async () => {
      loading.value = true
      try {
        const response = await ratingService.getRatingsByMentor(props.mentorId, {
          page: currentPage.value,
          limit: pageSize.value
        })

        if (response.code === 0) {
          ratings.value = response.data || []
          total.value = response.total || 0
        } else {
          message.error(response.message || '获取评价失败')
        }
      } catch (error) {
        message.error('获取评价失败')
        console.error('获取评价失败:', error)
      } finally {
        loading.value = false
      }
    }

    const changeSortBy = (field) => {
      if (sortBy.value !== field) {
        sortBy.value = field
        currentPage.value = 1
        fetchRatings()
      }
    }

    const handlePageChange = (page) => {
      currentPage.value = page
      fetchRatings()
    }

    const formatDate = (date) => {
      return dayjs(date).format('YYYY-MM-DD HH:mm')
    }

    const isOwnRating = (rating) => {
      if (!isAuthenticated.value || !currentUser.value) return false
      // 管理员或评价归属用户可以编辑/删除
      return userRole.value === 'ADMIN' || rating.studentId === currentUser.value.id
    }

    const handleLike = async (rating) => {
      if (!isAuthenticated.value) {
        message.warning('请先登录')
        return
      }

      try {
        const response = await ratingService.markRatingHelpful(rating.id)
        if (response.code === 0) {
          message.success('点赞成功')
          likedRatings.value.add(rating.id)
          rating.helpfulCount = (rating.helpfulCount || 0) + 1
        } else {
          message.error(response.message || '点赞失败')
        }
      } catch (error) {
        message.error('点赞失败')
        console.error('点赞失败:', error)
      }
    }

    const handleEdit = (rating) => {
      editingRating.value = { ...rating }
      editForm.rating = rating.rating
      editForm.comment = rating.comment

      // 分解评分：rating = integerRating * 2 + decimalRating / 5
      const integerPart = Math.floor(rating.rating)  // 整数部分（分数）
      const decimalPart = rating.rating - integerPart  // 小数部分（分数）

      if (integerPart % 2 === 0) {
        // 整数部分是偶数
        integerRating.value = integerPart / 2
      } else {
        // 整数部分是奇数
        integerRating.value = (integerPart - 1) / 2 + 0.5
      }

      decimalRating.value = decimalPart * 5

      editModalVisible.value = true
    }

    const handleDelete = async (rating) => {
      try {
        const response = await ratingService.deleteRating(rating.id)
        if (response.code === 0) {
          message.success('删除成功')
          fetchRatings()
        } else {
          message.error(response.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
        console.error('删除失败:', error)
      }
    }

    const handleIntegerChange = (value) => {
      integerRating.value = value
      const intScore = integerRating.value * 2
      decimalRating.value = 0
      editForm.rating = Number(intScore.toFixed(1))
    }

    const handleDecimalChange = (value) => {
      // 处理边界情况
      if (value === 5) {
        if (integerRating.value < 5) {
          integerRating.value += 0.5
          decimalRating.value = 0
          const intScore = integerRating.value * 2
          editForm.rating = Number(intScore.toFixed(1))
          return
        } else {
          value = 0
          decimalRating.value = 0
          message.warning('满分10分时不能再添加小数评分')
        }
      }

      if (value > 4.5) {
        value = 4.5
        decimalRating.value = 4.5
      }

      if (integerRating.value === 5) {
        value = 0
        decimalRating.value = 0
        message.warning('满分10分时不能再添加小数评分')
      }

      decimalRating.value = value
      const intScore = integerRating.value * 2
      const decimalScore = decimalRating.value / 5
      editForm.rating = Number((intScore + decimalScore).toFixed(1))
    }

    const handleEditSubmit = async () => {
      if (!editForm.comment || !editForm.comment.trim()) {
        message.warning('请填写评价内容')
        return
      }

      if (editForm.rating < 0.1 || editForm.rating > 10.0) {
        message.warning('评分必须在0.1-10.0之间')
        return
      }

      submitting.value = true
      try {
        const response = await ratingService.updateRating(editingRating.value.id, {
          rating: editForm.rating,
          comment: editForm.comment
        })

        if (response.code === 0) {
          message.success('修改成功')
          editModalVisible.value = false
          fetchRatings()
        } else {
          message.error(response.message || '修改失败')
        }
      } catch (error) {
        message.error('修改失败')
        console.error('修改失败:', error)
      } finally {
        submitting.value = false
      }
    }

    const handleEditCancel = () => {
      editModalVisible.value = false
      editingRating.value = null
      editForm.rating = 0
      editForm.comment = ''
      integerRating.value = 0
      decimalRating.value = 0
    }

    onMounted(() => {
      fetchRatings()
    })

    watch(() => props.mentorId, () => {
      currentPage.value = 1
      fetchRatings()
    })

    return {
      loading,
      ratings,
      total,
      currentPage,
      pageSize,
      sortBy,
      likedRatings,
      currentUser,
      isAuthenticated,
      userRole,
      changeSortBy,
      handlePageChange,
      formatDate,
      isOwnRating,
      handleLike,
      handleEdit,
      handleDelete,
      editModalVisible,
      editForm,
      integerRating,
      decimalRating,
      submitting,
      handleIntegerChange,
      handleDecimalChange,
      handleEditSubmit,
      handleEditCancel,
      refresh: fetchRatings
    }
  }
})
</script>

<style scoped>
.mentor-rating-list {
  margin-top: 24px;
}

.rating-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.rating-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.sort-controls {
  display: flex;
  gap: 8px;
}

.ratings-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rating-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.rating-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.rating-score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-number {
  font-size: 24px;
  font-weight: bold;
  color: #ff6b35;
}

.rating-date {
  color: #999;
  font-size: 12px;
}

.rating-content {
  margin-bottom: 12px;
  line-height: 1.6;
  color: #333;
}

.rating-content :deep(img) {
  max-width: 100%;
  height: auto;
}

.rating-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #666;
  font-size: 12px;
  margin-top: 8px;
}

.rating-actions {
  display: flex;
  gap: 8px;
}

.helpful-actions {
  display: flex;
  align-items: center;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}

.edit-form {
  padding: 16px 0;
}

.form-item {
  margin-bottom: 24px;
}

.form-label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.rating-input {
  background: #fafafa;
  padding: 16px;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.rating-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.rating-row:last-child {
  margin-bottom: 0;
}

.rating-label {
  min-width: 120px;
  color: #666;
  font-size: 14px;
}

.rating-value {
  color: #1890ff;
  font-weight: 500;
  min-width: 50px;
}

.rating-total {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e8e8e8;
  font-size: 16px;
  color: #333;
}

.rating-total strong {
  color: #ff6b35;
  font-size: 20px;
}
</style>
