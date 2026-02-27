<template>
  <div class="user-management">
    <a-card title="用户管理" :bordered="false">
      <!-- 搜索和筛选 -->
      <div class="search-section">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索用户名、邮箱..."
          style="width: 300px"
          @search="handleSearch"
        />
        <a-button type="primary" @click="$router.push('/admin/batch-import')" style="margin-left: 12px">
          📥 批量导入
        </a-button>
      </div>

      <!-- 用户列表表格 -->
      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userType'">
            <a-tag :color="getUserTypeColor(record.userType)">
              {{ getUserTypeText(record.userType) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 1 ? 'green' : 'red'">
              {{ record.status === 1 ? '正常' : '禁用' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showEditModal(record)">
                编辑
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 编辑用户模态框 -->
    <a-modal
      v-model:visible="modalVisible"
      title="编辑用户"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formState.username" placeholder="请输入用户名" />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
        </a-form-item>

        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formState.phone" placeholder="请输入手机号" />
        </a-form-item>

        <a-form-item label="用户类型">
          <a-input v-model:value="userTypeText" disabled />
        </a-form-item>

        <a-form-item label="账号状态">
          <a-tag :color="formState.status === 1 ? 'green' : 'red'">
            {{ formState.status === 1 ? '正常' : '禁用' }}
          </a-tag>
        </a-form-item>

        <a-form-item label="注册时间">
          <a-input v-model:value="formState.createTime" disabled />
        </a-form-item>

        <a-form-item label="最后登录时间">
          <a-input v-model:value="formState.lastLoginTime" disabled />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import userService from '../../service/userService'

export default defineComponent({
  name: 'UserManagement',
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const modalVisible = ref(false)
    const searchKeyword = ref('')
    const formRef = ref()

    const users = ref([])
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条记录`
    })

    const formState = reactive({
      id: null,
      username: '',
      email: '',
      phone: '',
      userType: '',
      status: null,
      createTime: '',
      lastLoginTime: ''
    })

    const userTypeText = computed(() => {
      const typeMap = {
        'ADMIN': '管理员',
        'MENTOR': '导师',
        'STUDENT': '学生'
      }
      return typeMap[formState.userType] || formState.userType
    })

    const columns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
      },
      {
        title: '用户名',
        dataIndex: 'username',
        key: 'username'
      },
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email'
      },
      {
        title: '手机号',
        dataIndex: 'phone',
        key: 'phone'
      },
      {
        title: '用户类型',
        key: 'userType',
        width: 100
      },
      {
        title: '状态',
        key: 'status',
        width: 80
      },
      {
        title: '注册时间',
        dataIndex: 'createTime',
        key: 'createTime',
        width: 180
      },
      {
        title: '操作',
        key: 'action',
        width: 100
      }
    ]

    const rules = {
      username: [
        { required: true, message: '请输入用户名', trigger: 'blur' },
        { min: 3, max: 50, message: '用户名长度应在3-50个字符之间', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' }
      ]
    }

    const getUserTypeText = (userType) => {
      const typeMap = {
        'ADMIN': '管理员',
        'MENTOR': '导师',
        'STUDENT': '学生'
      }
      return typeMap[userType] || userType
    }

    const getUserTypeColor = (userType) => {
      const colorMap = {
        'ADMIN': 'red',
        'MENTOR': 'blue',
        'STUDENT': 'green'
      }
      return colorMap[userType] || 'default'
    }

    const fetchUsers = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.current,
          limit: pagination.pageSize
        }
        const response = await userService.getAllUsers(params)
        if (response.code === 0) {
          users.value = response.data
          pagination.total = response.total
        } else {
          message.error(response.message || '获取用户列表失败')
        }
      } catch (error) {
        message.error('获取用户列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = async () => {
      if (!searchKeyword.value.trim()) {
        fetchUsers()
        return
      }

      loading.value = true
      try {
        // Filter users locally for now
        const keyword = searchKeyword.value.toLowerCase()
        const allUsers = users.value
        users.value = allUsers.filter(user =>
          user.username.toLowerCase().includes(keyword) ||
          (user.email && user.email.toLowerCase().includes(keyword))
        )
      } catch (error) {
        message.error('搜索失败')
      } finally {
        loading.value = false
      }
    }

    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchUsers()
    }

    const showEditModal = (record) => {
      Object.assign(formState, record)
      if (!formState.lastLoginTime) {
        formState.lastLoginTime = '从未登录'
      }
      modalVisible.value = true
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        submitting.value = true

        const updateData = {
          username: formState.username,
          email: formState.email,
          phone: formState.phone
        }

        const response = await userService.updateUserProfileByAdmin(formState.id, updateData)
        if (response.code === 0) {
          message.success('用户信息更新成功')
          modalVisible.value = false
          fetchUsers()
        } else {
          message.error(response.message || '更新失败')
        }
      } catch (error) {
        console.error('表单验证失败:', error)
      } finally {
        submitting.value = false
      }
    }

    onMounted(() => {
      fetchUsers()
    })

    return {
      loading,
      submitting,
      modalVisible,
      searchKeyword,
      formRef,
      users,
      pagination,
      formState,
      userTypeText,
      columns,
      rules,
      getUserTypeText,
      getUserTypeColor,
      handleSearch,
      handleTableChange,
      showEditModal,
      handleSubmit
    }
  }
})
</script>

<style scoped>
.user-management {
  padding: 24px;
}

.search-section {
  margin-bottom: 16px;
}
</style>
