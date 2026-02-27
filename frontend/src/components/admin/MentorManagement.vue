<template>
  <div class="mentor-management">
    <a-card title="导师管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <PlusOutlined /> 添加导师
        </a-button>
      </template>

      <!-- 搜索和筛选 -->
      <div class="search-section">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索导师姓名、邮箱..."
          style="width: 300px"
          @search="handleSearch"
        />
      </div>

      <!-- 导师列表表格 -->
      <a-table
        :columns="columns"
        :data-source="mentors"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="goToMentorDetail(record.id)" style="color: #1890ff; cursor: pointer;">
              {{ record.name }}
            </a>
          </template>
          <template v-else-if="column.key === 'researchAreas'">
            <span v-if="!record.researchAreas || record.researchAreas === 'null'">-</span>
            <span v-else>
              {{ parseResearchAreas(record.researchAreas) }}
            </span>
          </template>
          <template v-else-if="column.key === 'acceptingStudents'">
            <a-switch
              v-model:checked="record.acceptingStudents"
              @change="handleAcceptingStudentsChange(record)"
              :loading="record.updating"
            />
            <span style="margin-left: 8px;">
              {{ record.acceptingStudents ? '接收中' : '已关闭' }}
            </span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showEditModal(record)">
                编辑
              </a-button>
              <a-button type="link" size="small" @click="goToMentorDetail(record.id)">
                详情
              </a-button>
              <a-popconfirm
                title="确定要删除这位导师吗？"
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

    <!-- 添加/编辑导师模态框 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑导师' : '添加导师'"
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
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入导师姓名" />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
        </a-form-item>

        <a-form-item label="机构" name="institution">
          <a-input v-model:value="formState.institution" placeholder="请输入所属机构" />
        </a-form-item>

        <a-form-item label="研究方向" name="researchAreas">
          <a-select
            v-model:value="formState.researchAreas"
            mode="multiple"
            placeholder="请选择研究方向（可多选）"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="keyword in keywords" :key="keyword.id" :value="keyword.name">
              {{ keyword.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="关键词" name="keywords">
          <a-select
            v-model:value="formState.keywords"
            mode="tags"
            placeholder="请输入关键词（可自定义）"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option v-for="keyword in keywords" :key="keyword.id" :value="keyword.name">
              {{ keyword.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="个人简介" name="bio">
          <a-textarea
            v-model:value="formState.bio"
            placeholder="请输入个人简介"
            :rows="4"
          />
        </a-form-item>

        <a-form-item label="是否接收学生" name="acceptingStudents">
          <a-switch v-model:checked="formState.acceptingStudents" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import mentorService from '../../service/mentorService'
import keywordService from '../../service/keywordService'

export default defineComponent({
  name: 'MentorManagement',
  components: {
    PlusOutlined
  },
  setup() {
    const router = useRouter()
    const loading = ref(false)
    const submitting = ref(false)
    const modalVisible = ref(false)
    const isEdit = ref(false)
    const searchKeyword = ref('')
    const formRef = ref()

    const mentors = ref([])
    const keywords = ref([])
    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条记录`
    })

    const formState = reactive({
      id: null,
      name: '',
      email: '',
      institution: '',
      researchAreas: [],
      keywords: [],
      bio: '',
      acceptingStudents: true
    })

    const columns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
      },
      {
        title: '姓名',
        dataIndex: 'name',
        key: 'name',
        width: 120
      },
      {
        title: '邮箱',
        dataIndex: 'email',
        key: 'email'
      },
      {
        title: '机构',
        dataIndex: 'institution',
        key: 'institution'
      },
      {
        title: '研究方向',
        dataIndex: 'researchAreas',
        key: 'researchAreas',
        customRender: ({ text }) => {
          if (!text) return '-'
          try {
            const areas = JSON.parse(text)
            return Array.isArray(areas) ? areas.join(', ') : text
          } catch {
            return text
          }
        }
      },
      {
        title: '接收学生',
        key: 'acceptingStudents',
        width: 150
      },
      {
        title: '操作',
        key: 'action',
        width: 180,
        fixed: 'right'
      }
    ]

    const rules = {
      name: [
        { required: true, message: '请输入导师姓名', trigger: 'blur' }
      ],
      email: [
        { required: true, message: '请输入邮箱', trigger: 'blur' },
        { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
      ],
      institution: [
        { required: true, message: '请输入所属机构', trigger: 'blur' }
      ]
    }

    const fetchMentors = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.current,
          limit: pagination.pageSize
        }
        const response = await mentorService.getMentors(params)
        if (response.code === 0) {
          mentors.value = response.data
          pagination.total = response.total
        }
      } catch (error) {
        message.error('获取导师列表失败')
      } finally {
        loading.value = false
      }
    }

    const fetchKeywords = async () => {
      try {
        const response = await keywordService.getAllKeywords()
        if (response.code === 0) {
          keywords.value = response.data
        }
      } catch (error) {
        message.error('获取关键词列表失败')
      }
    }

    const filterOption = (input, option) => {
      return option.value.toLowerCase().indexOf(input.toLowerCase()) >= 0
    }

    const handleSearch = async () => {
      if (!searchKeyword.value.trim()) {
        fetchMentors()
        return
      }

      loading.value = true
      try {
        const params = {
          keyword: searchKeyword.value,
          page: pagination.current,
          limit: pagination.pageSize
        }
        const response = await mentorService.searchMentors(params)
        if (response.code === 0) {
          mentors.value = response.data
          pagination.total = response.total
        }
      } catch (error) {
        message.error('搜索失败')
      } finally {
        loading.value = false
      }
    }

    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchMentors()
    }

    const showAddModal = () => {
      console.log('showAddModal called')
      console.log('Before - modalVisible:', modalVisible.value)
      isEdit.value = false
      resetForm()
      modalVisible.value = true
      console.log('After - modalVisible:', modalVisible.value)
    }

    const showEditModal = (record) => {
      isEdit.value = true
      Object.assign(formState, {
        ...record,
        researchAreas: record.researchAreas ? (typeof record.researchAreas === 'string' ? JSON.parse(record.researchAreas) : record.researchAreas) : [],
        keywords: record.keywords ? (typeof record.keywords === 'string' ? JSON.parse(record.keywords) : record.keywords) : []
      })
      modalVisible.value = true
    }

    const resetForm = () => {
      formState.id = null
      formState.name = ''
      formState.email = ''
      formState.institution = ''
      formState.researchAreas = []
      formState.keywords = []
      formState.bio = ''
      formState.acceptingStudents = true
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        submitting.value = true

        // Convert arrays to JSON strings for backend
        const submitData = {
          ...formState,
          researchAreas: Array.isArray(formState.researchAreas) ? JSON.stringify(formState.researchAreas) : formState.researchAreas,
          keywords: Array.isArray(formState.keywords) ? JSON.stringify(formState.keywords) : formState.keywords
        }

        if (isEdit.value) {
          const response = await mentorService.updateMentor(submitData.id, submitData)
          if (response.code === 0) {
            message.success('更新成功')
            modalVisible.value = false
            fetchMentors()
          } else {
            message.error(response.message || '更新失败')
          }
        } else {
          const response = await mentorService.createMentor(submitData)
          if (response.code === 0) {
            message.success('添加成功')
            modalVisible.value = false
            fetchMentors()
          } else {
            message.error(response.message || '添加失败')
          }
        }
      } catch (error) {
        console.error('表单验证失败:', error)
      } finally {
        submitting.value = false
      }
    }

    const handleDelete = async (id) => {
      try {
        const response = await mentorService.deleteMentor(id)
        if (response.code === 0) {
          message.success('删除成功')
          fetchMentors()
        } else {
          message.error(response.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }

    const goToMentorDetail = (id) => {
      router.push(`/mentors/${id}`)
    }

    const parseResearchAreas = (researchAreas) => {
      if (!researchAreas || researchAreas === 'null') return '-'
      try {
        const areas = JSON.parse(researchAreas)
        return Array.isArray(areas) ? areas.join(', ') : researchAreas
      } catch {
        return researchAreas
      }
    }

    const handleAcceptingStudentsChange = async (record) => {
      try {
        record.updating = true
        const response = await mentorService.updateMentor(record.id, {
          acceptingStudents: record.acceptingStudents
        })
        if (response.code === 0) {
          message.success('更新成功')
        } else {
          // Revert on failure
          record.acceptingStudents = !record.acceptingStudents
          message.error(response.message || '更新失败')
        }
      } catch (error) {
        // Revert on error
        record.acceptingStudents = !record.acceptingStudents
        message.error('更新失败')
      } finally {
        record.updating = false
      }
    }

    onMounted(() => {
      fetchMentors()
      fetchKeywords()
    })

    return {
      loading,
      submitting,
      modalVisible,
      isEdit,
      searchKeyword,
      formRef,
      mentors,
      keywords,
      pagination,
      formState,
      columns,
      rules,
      filterOption,
      handleSearch,
      handleTableChange,
      showAddModal,
      showEditModal,
      handleSubmit,
      handleDelete,
      goToMentorDetail,
      parseResearchAreas,
      handleAcceptingStudentsChange
    }
  }
})
</script>

<style scoped>
.mentor-management {
  padding: 24px;
}

.search-section {
  margin-bottom: 16px;
}
</style>
