<template>
  <div class="admin-dashboard">
    <a-page-header title="管理员控制台" @back="() => $router.push('/mentors')">
      <template #extra>
        <a-button @click="handleLogout">退出登录</a-button>
      </template>
    </a-page-header>

    <a-row :gutter="24">
      <a-col :span="6">
        <a-card>
          <a-statistic title="总用户数" :value="stats.totalUsers" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="导师数" :value="stats.totalMentors" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="学生数" :value="stats.totalStudents" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic title="申请数" :value="stats.totalApplications" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="最近申请" class="recent-applications">
      <a-table
        :columns="applicationColumns"
        :data-source="recentApplications"
        :loading="loading"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ formatDate(record.createTime) }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import applicationService from '../../service/applicationService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'AdminDashboard',
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)
    const stats = ref({
      totalUsers: 0,
      totalMentors: 0,
      totalStudents: 0,
      totalApplications: 0
    })
    const recentApplications = ref([])

    const applicationColumns = [
      { title: '学生', dataIndex: 'studentName', key: 'studentName' },
      { title: '导师', dataIndex: 'mentorName', key: 'mentorName' },
      { title: '状态', key: 'status' },
      { title: '申请时间', key: 'createTime' }
    ]

    const fetchStats = async () => {
      // Mock data - replace with actual API calls
      stats.value = {
        totalUsers: 150,
        totalMentors: 45,
        totalStudents: 105,
        totalApplications: 230
      }
    }

    const fetchRecentApplications = async () => {
      loading.value = true
      try {
        const response = await applicationService.getApplications({ page: 1, limit: 10 })
        if (response.code === 0) {
          recentApplications.value = response.data
        }
      } catch (error) {
        message.error('获取申请列表失败')
      } finally {
        loading.value = false
      }
    }

    const getStatusColor = (status) => {
      const colors = {
        pending: 'processing',
        accepted: 'success',
        rejected: 'error',
        withdrawn: 'default'
      }
      return colors[status] || 'default'
    }

    const getStatusText = (status) => {
      const texts = {
        pending: '待处理',
        accepted: '已接受',
        rejected: '已拒绝',
        withdrawn: '已撤回'
      }
      return texts[status] || status
    }

    const formatDate = (date) => {
      return dayjs(date).format('YYYY-MM-DD HH:mm')
    }

    const handleLogout = async () => {
      try {
        await store.dispatch('auth/logout')
        message.success('已退出登录')
        router.push('/login')
      } catch (error) {
        message.error('退出失败')
      }
    }

    onMounted(() => {
      fetchStats()
      fetchRecentApplications()
    })

    return {
      loading,
      stats,
      recentApplications,
      applicationColumns,
      getStatusColor,
      getStatusText,
      formatDate,
      handleLogout
    }
  }
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px;
}

.recent-applications {
  margin-top: 24px;
}
</style>
