<template>
  <div class="rating-management">
    <a-card title="评价管理" bordered="false">
      <div class="search-section">
        <a-space>
          <a-input-search
            v-model:value="searchMentorId"
            placeholder="按导师ID搜索"
            style="width: 200px"
            @search="handleSearch"
          />
          <a-input-search
            v-model:value="searchStudentId"
            placeholder="按学生ID搜索"
            style="width: 200px"
            @search="handleSearch"
          />
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="ratings"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'rating'">
            <a-rate :value="record.rating" disabled />
          </template>
          <template v-if="column.key === 'isAnonymous'">
            <a-tag :color="record.isAnonymous ? 'blue' : 'default'">
              {{ record.isAnonymous ? '匿名' : '实名' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'isVerified'">
            <a-tag :color="record.isVerified ? 'green' : 'default'">
              {{ record.isVerified ? '已认证' : '未认证' }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showDetailModal(record)">
                查看详情
              </a-button>
              <a-popconfirm
                title="确定要删除这条评价吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" danger size="small">
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:visible="detailModalVisible"
      title="评价详情"
      :footer="null"
      width="600px"
    >
      <a-descriptions bordered :column="1" v-if="selectedRating">
        <a-descriptions-item label="评价ID">
          {{ selectedRating.id }}
        </a-descriptions-item>
        <a-descriptions-item label="导师ID">
          {{ selectedRating.mentorId }}
        </a-descriptions-item>
        <a-descriptions-item label="学生ID">
          {{ selectedRating.studentId }}
        </a-descriptions-item>
        <a-descriptions-item label="评分">
          <a-rate :value="selectedRating.rating" disabled />
        </a-descriptions-item>
        <a-descriptions-item label="评价内容">
          {{ selectedRating.comment || '无' }}
        </a-descriptions-item>
        <a-descriptions-item label="是否匿名">
          <a-tag :color="selectedRating.isAnonymous ? 'blue' : 'default'">
            {{ selectedRating.isAnonymous ? '匿名' : '实名' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="是否认证">
          <a-tag :color="selectedRating.isVerified ? 'green' : 'default'">
            {{ selectedRating.isVerified ? '已认证' : '未认证' }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="有用数">
          {{ selectedRating.helpfulCount || 0 }}
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ selectedRating.createTime }}
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import ratingService from '../../service/ratingService'

export default defineComponent({
  name: 'RatingManagement',
  setup() {
    const loading = ref(false)
    const detailModalVisible = ref(false)
    const searchMentorId = ref('')
    const searchStudentId = ref('')
    const ratings = ref([])
    const selectedRating = ref(null)

    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条记录`
    })

    const columns = [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
      { title: '导师ID', dataIndex: 'mentorId', key: 'mentorId', width: 100 },
      { title: '学生ID', dataIndex: 'studentId', key: 'studentId', width: 100 },
      { title: '评分', key: 'rating', width: 150 },
      { title: '评价内容', dataIndex: 'comment', key: 'comment', ellipsis: true },
      { title: '匿名', key: 'isAnonymous', width: 80 },
      { title: '认证', key: 'isVerified', width: 80 },
      { title: '有用数', dataIndex: 'helpfulCount', key: 'helpfulCount', width: 100 },
      { title: '操作', key: 'action', width: 150 }
    ]

    const fetchRatings = async (mentorId = null, studentId = null) => {
      loading.value = true
      try {
        let response
        if (mentorId) {
          response = await ratingService.getRatingsByMentor(mentorId, {
            page: pagination.current,
            limit: pagination.pageSize
          })
        } else if (studentId) {
          response = await ratingService.getRatingsByStudent(studentId)
        } else {
          // 如果没有提供ID，获取所有评价（需要后端支持）
          response = { code: 0, data: [], total: 0 }
        }

        if (response.code === 0) {
          ratings.value = response.data || []
          pagination.total = response.total || 0
        }
      } catch (error) {
        message.error('获取评价列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = () => {
      if (searchMentorId.value.trim()) {
        fetchRatings(parseInt(searchMentorId.value), null)
      } else if (searchStudentId.value.trim()) {
        fetchRatings(null, parseInt(searchStudentId.value))
      } else {
        fetchRatings()
      }
    }

    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      if (searchMentorId.value.trim()) {
        fetchRatings(parseInt(searchMentorId.value), null)
      } else if (searchStudentId.value.trim()) {
        fetchRatings(null, parseInt(searchStudentId.value))
      } else {
        fetchRatings()
      }
    }

    const showDetailModal = (record) => {
      selectedRating.value = record
      detailModalVisible.value = true
    }

    const handleDelete = async (id) => {
      try {
        // 注意：ratingService 可能没有 deleteRating 方法，需要添加
        message.warning('删除功能需要后端支持')
        // const response = await ratingService.deleteRating(id)
        // if (response.code === 0) {
        //   message.success('删除成功')
        //   handleSearch()
        // } else {
        //   message.error(response.message || '删除失败')
        // }
      } catch (error) {
        message.error('删除失败')
      }
    }

    onMounted(() => {
      fetchRatings()
    })

    return {
      loading,
      detailModalVisible,
      searchMentorId,
      searchStudentId,
      ratings,
      selectedRating,
      pagination,
      columns,
      handleSearch,
      handleTableChange,
      showDetailModal,
      handleDelete
    }
  }
})
</script>

<style scoped>
.rating-management {
  padding: 24px;
}

.search-section {
  margin-bottom: 16px;
}
</style>
