<template>
  <div class="publication-management">
    <a-card title="学术成果管理" bordered="false">
      <template #extra>
        <a-button type="primary" @click="showAddModal">
          <PlusOutlined />
          添加学术成果
        </a-button>
      </template>

      <div class="search-section">
        <a-input-search
          v-model:value="searchKeyword"
          placeholder="搜索标题、作者、关键词..."
          style="width: 300px"
          @search="handleSearch"
        />
      </div>

      <a-table
        :columns="columns"
        :data-source="publications"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="showEditModal(record)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除这篇论文吗？"
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
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑学术成果' : '添加学术成果'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      width="800px"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="导师ID" name="mentorId">
          <a-input-number
            v-model:value="formState.mentorId"
            placeholder="请输入导师ID"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="论文标题" name="title">
          <a-input
            v-model:value="formState.title"
            placeholder="请输入论文标题"
          />
        </a-form-item>

        <a-form-item label="作者列表" name="selectedAuthors">
          <ScholarSelector v-model="formState.selectedAuthors" />
        </a-form-item>

        <a-form-item label="发表会议/期刊" name="venue">
          <a-input
            v-model:value="formState.venue"
            placeholder="请输入发表会议或期刊"
          />
        </a-form-item>

        <a-form-item label="发表年份" name="year">
          <a-input-number
            v-model:value="formState.year"
            placeholder="请输入发表年份"
            :min="1900"
            :max="2100"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="摘要" name="abstractText">
          <a-textarea
            v-model:value="formState.abstractText"
            placeholder="请输入摘要"
            :rows="4"
          />
        </a-form-item>

        <a-form-item label="DOI" name="doi">
          <a-input
            v-model:value="formState.doi"
            placeholder="请输入DOI"
          />
        </a-form-item>

        <a-form-item label="PDF链接" name="pdfUrl">
          <a-input
            v-model:value="formState.pdfUrl"
            placeholder="请输入PDF链接"
          />
        </a-form-item>

        <a-form-item label="类型" name="publicationType">
          <a-select
            v-model:value="formState.publicationType"
            placeholder="请选择类型"
          >
            <a-select-option value="Journal">期刊</a-select-option>
            <a-select-option value="Conference">会议</a-select-option>
            <a-select-option value="Workshop">研讨会</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import publicationService from '../../service/publicationService'
import ScholarSelector from '../scholar/ScholarSelector.vue'

export default defineComponent({
  name: 'PublicationManagement',
  components: {
    PlusOutlined,
    ScholarSelector
  },
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const modalVisible = ref(false)
    const isEdit = ref(false)
    const searchKeyword = ref('')
    const formRef = ref()
    const publications = ref([])

    const pagination = reactive({
      current: 1,
      pageSize: 10,
      total: 0,
      showSizeChanger: true,
      showTotal: total => `共 ${total} 条记录`
    })

    const formState = reactive({
      id: null,
      mentorId: null,
      title: '',
      authors: '',
      selectedAuthors: [],
      venue: '',
      year: null,
      abstractText: '',
      doi: '',
      pdfUrl: '',
      publicationType: 'Journal'
    })

    const columns = [
      { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
      { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
      { title: '作者', dataIndex: 'authors', key: 'authors', ellipsis: true },
      { title: '会议/期刊', dataIndex: 'venue', key: 'venue' },
      { title: '年份', dataIndex: 'year', key: 'year', width: 100 },
      { title: '类型', dataIndex: 'publicationType', key: 'publicationType', width: 100 },
      { title: '操作', key: 'action', width: 150 }
    ]

    const rules = {
      mentorId: [{ required: true, message: '请输入导师ID', trigger: 'blur' }],
      title: [{ required: true, message: '请输入论文标题', trigger: 'blur' }]
    }

    const fetchPublications = async () => {
      loading.value = true
      try {
        const params = {
          page: pagination.current,
          limit: pagination.pageSize
        }
        const response = await publicationService.searchPublications(params)
        if (response.code === 0) {
          publications.value = response.data
          pagination.total = response.total
        }
      } catch (error) {
        message.error('获取学术成果列表失败')
      } finally {
        loading.value = false
      }
    }

    const handleSearch = async () => {
      if (searchKeyword.value.trim()) {
        loading.value = true
        try {
          const params = {
            keyword: searchKeyword.value,
            page: pagination.current,
            limit: pagination.pageSize
          }
          const response = await publicationService.searchPublications(params)
          if (response.code === 0) {
            publications.value = response.data
            pagination.total = response.total
          }
        } catch (error) {
          message.error('搜索失败')
        } finally {
          loading.value = false
        }
      } else {
        fetchPublications()
      }
    }

    const handleTableChange = (pag) => {
      pagination.current = pag.current
      pagination.pageSize = pag.pageSize
      fetchPublications()
    }

    const showAddModal = () => {
      isEdit.value = false
      resetForm()
      modalVisible.value = true
    }

    const showEditModal = async (record) => {
      isEdit.value = true
      Object.assign(formState, record)

      // Load authors for this publication
      try {
        const response = await publicationService.getAuthorsForPublication(record.id)
        if (response.code === 0) {
          formState.selectedAuthors = response.data.map(author => ({
            id: author.scholarId,
            name: author.name,
            institution: author.institution,
            email: author.email,
            avatarUrl: author.avatarUrl,
            authorOrder: author.authorOrder,
            isCorresponding: author.isCorresponding
          }))
        }
      } catch (error) {
        console.error('加载作者列表失败:', error)
      }

      modalVisible.value = true
    }

    const resetForm = () => {
      formState.id = null
      formState.mentorId = null
      formState.title = ''
      formState.authors = ''
      formState.selectedAuthors = []
      formState.venue = ''
      formState.year = null
      formState.abstractText = ''
      formState.doi = ''
      formState.pdfUrl = ''
      formState.publicationType = 'Journal'
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        submitting.value = true

        if (isEdit.value) {
          const response = await publicationService.updatePublication(formState.id, formState)
          if (response.code === 0) {
            // Update author relationships
            await updatePublicationAuthors(formState.id)
            message.success('更新成功')
            modalVisible.value = false
            fetchPublications()
          } else {
            message.error(response.message || '更新失败')
          }
        } else {
          const response = await publicationService.createPublication(formState)
          if (response.code === 0) {
            // Add author relationships
            await updatePublicationAuthors(response.data.id)
            message.success('添加成功')
            modalVisible.value = false
            fetchPublications()
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

    const updatePublicationAuthors = async (publicationId) => {
      try {
        // Remove all existing authors first (for edit mode)
        if (isEdit.value) {
          // We'll just add new ones - the backend should handle duplicates
        }

        // Add selected authors
        for (const scholar of formState.selectedAuthors) {
          await publicationService.addAuthorToPublication(publicationId, {
            scholarId: scholar.id,
            authorOrder: scholar.authorOrder,
            isCorresponding: scholar.isCorresponding
          })
        }
      } catch (error) {
        console.error('保存作者关系失败:', error)
        message.warning('论文已保存，但作者关系保存失败')
      }
    }

    const handleDelete = async (id) => {
      try {
        const response = await publicationService.deletePublication(id)
        if (response.code === 0) {
          message.success('删除成功')
          fetchPublications()
        } else {
          message.error(response.message || '删除失败')
        }
      } catch (error) {
        message.error('删除失败')
      }
    }

    onMounted(() => {
      fetchPublications()
    })

    return {
      loading,
      submitting,
      modalVisible,
      isEdit,
      searchKeyword,
      formRef,
      publications,
      pagination,
      formState,
      columns,
      rules,
      handleSearch,
      handleTableChange,
      showAddModal,
      showEditModal,
      handleSubmit,
      handleDelete,
      updatePublicationAuthors
    }
  }
})
</script>

<style scoped>
.publication-management {
  padding: 24px;
}

.search-section {
  margin-bottom: 16px;
}
</style>
