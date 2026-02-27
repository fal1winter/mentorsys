<template>
  <div class="keyword-management">
    <a-card title="关键词管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <PlusOutlined /> 添加关键词
        </a-button>
      </template>

      <!-- 搜索和筛选 -->
      <div class="search-section">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索关键词名称、简介、分类..."
          style="width: 300px"
          @search="handleSearch"
        />
      </div>

      <!-- 关键词列表表格 -->
      <a-table
        :columns="columns"
        :data-source="keywords"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'description'">
            <div class="description-cell">
              {{ record.description || '-' }}
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showEditModal(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除这个关键词吗？"
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

    <!-- 添加/编辑关键词模态框 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑关键词' : '添加关键词'"
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
        <a-form-item label="关键词名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入关键词名称" />
        </a-form-item>

        <a-form-item label="关键词简介" name="description">
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入关键词简介"
            :rows="4"
          />
        </a-form-item>

        <a-form-item label="分类" name="category">
          <a-input v-model:value="formState.category" placeholder="请输入分类（如：人工智能、数据库、网络等）" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import keywordService from '../../service/keywordService'

export default defineComponent({
  name: 'KeywordManagement',
  components: {
    PlusOutlined
  },
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const modalVisible = ref(false)
    const isEdit = ref(false)
    const searchKeyword = ref('')
    const formRef = ref()

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
      description: '',
      category: ''
    })

    const columns = [
      {
        title: 'ID',
        dataIndex: 'id',
        key: 'id',
        width: 80
      },
      {
        title: '关键词名称',
        dataIndex: 'name',
        key: 'name',
        width: 150
      },
      {
        title: '简介',
        key: 'description',
        dataIndex: 'description',
        ellipsis: true
      },
      {
        title: '分类',
        dataIndex: 'category',
        key: 'category',
        width: 120
      },
      {
        title: '操作',
        key: 'action',
        width: 150
      }
    ]

    const rules = {
      name: [
        { required: true, message: '请输入关键词名称', trigger: 'blur' }
      ],
      category: [
        { required: false, message: '请输入分类', trigger: 'blur' }
      ]
    }

    const fetchKeywords = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.current,
          limit: pagination.pageSize
        }
        const response = await keywordService.getKeywords(params)
        if (response.code === 0) {
          keywords.value = response.data
          pagination.total = response.total
        }
      } catch (error) {
        message.error('获取关键词列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = async () => {
      if (!searchKeyword.value.trim()) {
        fetchKeywords()
        return
      }

      loading.value = true
      try {
        const params = {
          keyword: searchKeyword.value,
          page: pagination.current,
          limit: pagination.pageSize
        }
        const response = await keywordService.searchKeywords(params)
        if (response.code === 0) {
          keywords.value = response.data
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
      fetchKeywords()
    }

    const showAddModal = () => {
      isEdit.value = false
      resetForm()
      modalVisible.value = true
    }

    const showEditModal = (record) => {
      isEdit.value = true
      Object.assign(formState, record)
      modalVisible.value = true
    }

    const resetForm = () => {
      formState.id = null
      formState.name = ''
      formState.description = ''
      formState.category = ''
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        submitting.value = true

        if (isEdit.value) {
          const response = await keywordService.updateKeyword(formState.id, formState)
          if (response.code === 0) {
            message.success('更新成功')
            modalVisible.value = false
            fetchKeywords()
          } else {
            message.error(response.message || '更新失败')
          }
        } else {
          const response = await keywordService.createKeyword(formState)
          if (response.code === 0) {
            message.success('添加成功')
            modalVisible.value = false
            fetchKeywords()
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
        const response = await keywordService.deleteKeyword(id)
        if (response.code === 0) {
          message.success('删除成功')
          fetchKeywords()
        } else {
          message.error(response.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }

    onMounted(() => {
      fetchKeywords()
    })

    return {
      loading,
      submitting,
      modalVisible,
      isEdit,
      searchKeyword,
      formRef,
      keywords,
      pagination,
      formState,
      columns,
      rules,
      handleSearch,
      handleTableChange,
      showAddModal,
      showEditModal,
      handleSubmit,
      handleDelete
    }
  }
})
</script>

<style scoped>
.keyword-management {
  padding: 24px;
}

.search-section {
  margin-bottom: 16px;
}

.description-cell {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
