<template>
  <div class="mentor-publication-list">
    <div class="publication-header">
      <h3>学术成果 ({{ total }})</h3>
      <a-button
        v-if="canManage"
        type="primary"
        @click="toggleForm"
      >
        <PlusOutlined v-if="!showForm" />
        <span v-if="!showForm">添加成果</span>
        <span v-else>收起表单</span>
      </a-button>
    </div>

    <!-- 模态框表单 -->
    <a-modal
      v-model:visible="showForm"
      :title="isEdit ? '编辑学术成果' : '添加学术成果'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="cancelForm"
      width="700px"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="rules"
        layout="vertical"
      >
        <a-form-item label="论文标题" name="title">
          <a-input v-model:value="formState.title" placeholder="请输入论文标题" />
        </a-form-item>

        <a-form-item label="作者列表" name="authors">
          <a-input v-model:value="formState.authors" placeholder="请输入作者列表" />
        </a-form-item>

        <a-form-item label="发表会议/期刊" name="venue">
          <a-input v-model:value="formState.venue" placeholder="请输入发表会议或期刊" />
        </a-form-item>

        <a-form-item label="发表年份" name="year">
          <a-input-number
            v-model:value="formState.year"
            :min="1900"
            :max="2100"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="类型" name="publicationType">
          <a-select v-model:value="formState.publicationType">
            <a-select-option value="Journal">期刊</a-select-option>
            <a-select-option value="Conference">会议</a-select-option>
            <a-select-option value="Workshop">研讨会</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="摘要" name="abstractText">
          <a-textarea
            v-model:value="formState.abstractText"
            :rows="4"
            placeholder="请输入摘要"
          />
        </a-form-item>

        <a-form-item label="DOI" name="doi">
          <a-input v-model:value="formState.doi" placeholder="请输入DOI" />
        </a-form-item>

        <a-form-item label="PDF链接" name="pdfUrl">
          <a-input v-model:value="formState.pdfUrl" placeholder="请输入PDF链接" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-spin :spinning="loading">
      <a-empty v-if="publications.length === 0" description="暂无学术成果" />
      <div v-else class="publications-container">
        <div v-for="pub in publications" :key="pub.id" class="publication-item">
          <div class="pub-header">
            <h4 class="pub-title">{{ pub.title }}</h4>
            <div v-if="canManage" class="pub-actions">
              <a-button type="link" size="small" @click="showEditModal(pub)">
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除这篇论文吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(pub.id)"
              >
                <a-button type="link" danger size="small">
                  删除
                </a-button>
              </a-popconfirm>
            </div>
          </div>
          <div class="pub-authors">{{ pub.authors }}</div>
          <div class="pub-venue">
            <span class="venue-name">{{ pub.venue }}</span>
            <span class="pub-year">{{ pub.year }}</span>
            <a-tag v-if="pub.publicationType" color="blue">{{ pub.publicationType }}</a-tag>
          </div>
          <div v-if="pub.abstractText" class="pub-abstract">
            {{ pub.abstractText }}
          </div>
          <div class="pub-links">
            <a v-if="pub.doi" :href="`https://doi.org/${pub.doi}`" target="_blank">
              <LinkOutlined /> DOI
            </a>
            <a v-if="pub.pdfUrl" :href="pub.pdfUrl" target="_blank">
              <FilePdfOutlined /> PDF
            </a>
          </div>
        </div>
      </div>
    </a-spin>

    <div class="pagination-container" v-if="total > pageSize">
      <a-pagination
        v-model:current="currentPage"
        v-model:pageSize="pageSize"
        :total="total"
        @change="handlePageChange"
        :showTotal="(total) => `共 ${total} 篇`"
      />
    </div>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, LinkOutlined, FilePdfOutlined } from '@ant-design/icons-vue'
import publicationService from '../../service/publicationService'

export default defineComponent({
  name: 'MentorPublicationList',
  components: {
    PlusOutlined,
    LinkOutlined,
    FilePdfOutlined
  },
  props: {
    mentorId: {
      type: Number,
      required: true
    },
    canManage: {
      type: Boolean,
      default: false
    }
  },
  setup(props) {
    const loading = ref(false)
    const publications = ref([])
    const total = ref(0)
    const currentPage = ref(1)
    const pageSize = ref(10)
    const showForm = ref(false)
    const isEdit = ref(false)
    const submitting = ref(false)
    const formRef = ref()

    const formState = reactive({
      id: null,
      mentorId: props.mentorId,
      title: '',
      authors: '',
      venue: '',
      year: new Date().getFullYear(),
      abstractText: '',
      doi: '',
      pdfUrl: '',
      publicationType: 'Journal'
    })

    const rules = {
      title: [{ required: true, message: '请输入论文标题' }],
      authors: [{ required: true, message: '请输入作者列表' }]
    }

    const fetchPublications = async () => {
      loading.value = true
      try {
        const response = await publicationService.getPublicationsByMentor(props.mentorId, {
          page: currentPage.value,
          limit: pageSize.value
        })

        if (response.code === 0) {
          publications.value = response.data || []
          total.value = response.total || 0
        } else {
          message.error(response.message || '获取学术成果失败')
        }
      } catch (error) {
        message.error('获取学术成果失败')
        console.error('获取学术成果失败:', error)
      } finally {
        loading.value = false
      }
    }

    const toggleForm = () => {
      isEdit.value = false
      resetForm()
      showForm.value = true
    }

    const cancelForm = () => {
      showForm.value = false
      resetForm()
    }

    const showEditModal = (pub) => {
      isEdit.value = true
      Object.assign(formState, pub)
      showForm.value = true
    }

    const resetForm = () => {
      formState.id = null
      formState.mentorId = props.mentorId
      formState.title = ''
      formState.authors = ''
      formState.venue = ''
      formState.year = new Date().getFullYear()
      formState.abstractText = ''
      formState.doi = ''
      formState.pdfUrl = ''
      formState.publicationType = 'Journal'
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        submitting.value = true

        let response
        if (isEdit.value) {
          response = await publicationService.updatePublication(formState.id, formState)
        } else {
          response = await publicationService.createPublication(formState)
        }

        if (response.code === 0) {
          message.success(isEdit.value ? '更新成功' : '添加成功')
          showForm.value = false
          fetchPublications()
        } else {
          message.error(response.message || '操作失败')
        }
      } catch (error) {
        console.error('操作失败:', error)
      } finally {
        submitting.value = false
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

    const handlePageChange = (page) => {
      currentPage.value = page
      fetchPublications()
    }

    onMounted(() => {
      fetchPublications()
    })

    watch(() => props.mentorId, () => {
      currentPage.value = 1
      fetchPublications()
    })

    return {
      loading,
      publications,
      total,
      currentPage,
      pageSize,
      showForm,
      isEdit,
      submitting,
      formRef,
      formState,
      rules,
      toggleForm,
      cancelForm,
      showEditModal,
      handleSubmit,
      handleDelete,
      handlePageChange
    }
  }
})
</script>

<style scoped>
.mentor-publication-list {
  margin-top: 24px;
}

.publication-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.publication-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.publications-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.publication-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.pub-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.pub-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
  flex: 1;
}

.pub-actions {
  display: flex;
  gap: 8px;
}

.pub-authors {
  color: #666;
  margin-bottom: 4px;
}

.pub-venue {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  color: #999;
  font-size: 14px;
}

.venue-name {
  font-style: italic;
}

.pub-year {
  font-weight: 500;
}

.pub-abstract {
  margin-bottom: 8px;
  color: #666;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pub-links {
  display: flex;
  gap: 16px;
}

.pub-links a {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #1890ff;
}

.pagination-container {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
