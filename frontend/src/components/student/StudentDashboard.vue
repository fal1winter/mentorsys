<template>
  <div class="student-dashboard">
    <a-page-header title="学生中心" @back="() => $router.push('/mentors')">
      <template #extra>
        <a-button @click="() => $router.push('/student/profile')">编辑资料</a-button>
      </template>
    </a-page-header>

    <a-row :gutter="24">
      <a-col :span="24">
        <a-card title="我的申请" class="dashboard-card">
          <a-list
            :data-source="applications"
            :loading="loading"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <router-link :to="`/mentors/${item.mentorId}`">
                      {{ item.mentorName }}
                    </router-link>
                  </template>
                  <template #description>
                    <div>申请时间: {{ formatDate(item.createTime) }}</div>
                    <div>状态: <a-tag :color="getStatusColor(item.status)">{{ getStatusText(item.status) }}</a-tag></div>
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a-button
                    v-if="item.status === 'accepted'"
                    type="primary"
                    @click="() => $router.push(`/chat/${item.id}`)"
                  >
                    进入聊天
                  </a-button>
                  <a-button
                    v-if="item.status === 'pending'"
                    danger
                    @click="handleWithdraw(item.id)"
                  >
                    撤回申请
                  </a-button>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script>
import { defineComponent, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { message, Modal } from 'ant-design-vue'
import applicationService from '../../service/applicationService'
import dayjs from 'dayjs'

export default defineComponent({
  name: 'StudentDashboard',
  setup() {
    const store = useStore()
    const loading = ref(false)
    const applications = ref([])

    const currentUser = computed(() => store.getters['auth/currentUser'])

    const fetchApplications = async () => {
      loading.value = true
      try {
        const response = await applicationService.getApplications({
          studentId: currentUser.value.id
        })
        if (response.code === 0) {
          applications.value = response.data
        }
      } catch (error) {
        message.error('获取申请列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleWithdraw = (id) => {
      Modal.confirm({
        title: '确认撤回申请？',
        content: '撤回后将无法恢复',
        onOk: async () => {
          try {
            const response = await applicationService.withdrawApplication(id)
            if (response.code === 0) {
              message.success('已撤回申请')
              fetchApplications()
            }
          } catch (error) {
            message.error('撤回失败')
          }
        }
      })
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

    onMounted(() => {
      fetchApplications()
    })

    return {
      loading,
      applications,
      handleWithdraw,
      getStatusColor,
      getStatusText,
      formatDate
    }
  }
})
</script>

<style scoped>
.student-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.dashboard-card {
  margin-bottom: 24px;
}
</style>
