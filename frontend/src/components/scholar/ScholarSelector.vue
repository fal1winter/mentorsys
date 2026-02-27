<template>
  <div class="scholar-selector">
    <div class="selected-scholars">
      <h4>已选择的作者</h4>
      <a-list
        v-if="selectedScholars.length > 0"
        :data-source="selectedScholars"
        size="small"
      >
        <template #renderItem="{ item, index }">
          <a-list-item>
            <template #actions>
              <a-button type="link" size="small" @click="moveUp(index)" :disabled="index === 0">
                上移
              </a-button>
              <a-button type="link" size="small" @click="moveDown(index)" :disabled="index === selectedScholars.length - 1">
                下移
              </a-button>
              <a-button type="link" danger size="small" @click="removeScholar(index)">
                移除
              </a-button>
            </template>
            <a-list-item-meta>
              <template #title>
                {{ item.name }}
                <a-tag v-if="item.isCorresponding" color="blue">通讯作者</a-tag>
              </template>
              <template #description>
                {{ item.institution || '未填写机构' }}
              </template>
            </a-list-item-meta>
            <template #extra>
              <a-checkbox v-model:checked="item.isCorresponding" @change="updateScholar(index)">
                通讯作者
              </a-checkbox>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else description="暂无选择的作者" :image="simpleImage" />
    </div>

    <a-divider />

    <div class="search-section">
      <h4>搜索学者</h4>
      <a-input-search
        v-model:value="searchKeyword"
        placeholder="搜索学者姓名、机构..."
        @search="handleSearch"
        style="margin-bottom: 16px"
      />

      <a-spin :spinning="searching">
        <a-list
          v-if="searchResults.length > 0"
          :data-source="searchResults"
          size="small"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <template #actions>
                <a-button
                  type="primary"
                  size="small"
                  @click="addScholar(item)"
                  :disabled="isScholarSelected(item.id)"
                >
                  {{ isScholarSelected(item.id) ? '已添加' : '添加' }}
                </a-button>
              </template>
              <a-list-item-meta>
                <template #title>{{ item.name }}</template>
                <template #description>
                  {{ item.institution || '未填写机构' }}
                  <span v-if="item.email"> · {{ item.email }}</span>
                </template>
              </a-list-item-meta>
            </a-list-item>
          </template>
        </a-list>
        <a-empty v-else-if="searchKeyword" description="未找到匹配的学者" :image="simpleImage" />
      </a-spin>
    </div>

    <a-divider />

    <div class="create-section">
      <h4>快速创建学者</h4>
      <a-button type="dashed" block @click="showCreateModal">
        <PlusOutlined />
        创建新学者
      </a-button>
    </div>

    <a-modal
      v-model:visible="createModalVisible"
      title="创建新学者"
      :confirm-loading="creating"
      @ok="handleCreateScholar"
    >
      <a-form
        ref="createFormRef"
        :model="createFormState"
        :rules="createRules"
        layout="vertical"
      >
        <a-form-item label="姓名" name="name">
          <a-input v-model:value="createFormState.name" placeholder="请输入学者姓名" />
        </a-form-item>

        <a-form-item label="机构" name="institution">
          <a-input v-model:value="createFormState.institution" placeholder="请输入所属机构" />
        </a-form-item>

        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="createFormState.email" placeholder="请输入邮箱" />
        </a-form-item>

        <a-form-item label="个人主页" name="homepage">
          <a-input v-model:value="createFormState.homepage" placeholder="请输入个人主页URL" />
        </a-form-item>

        <a-form-item label="简介" name="bio">
          <a-textarea
            v-model:value="createFormState.bio"
            placeholder="请输入个人简介"
            :rows="3"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, watch } from 'vue'
import { message, Empty } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import scholarService from '../../service/scholarService'

export default defineComponent({
  name: 'ScholarSelector',
  components: {
    PlusOutlined
  },
  props: {
    modelValue: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    const searchKeyword = ref('')
    const searching = ref(false)
    const searchResults = ref([])
    const selectedScholars = ref([...props.modelValue])
    const createModalVisible = ref(false)
    const creating = ref(false)
    const createFormRef = ref()
    const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE

    const createFormState = reactive({
      name: '',
      institution: '',
      email: '',
      homepage: '',
      bio: ''
    })

    const createRules = {
      name: [{ required: true, message: '请输入学者姓名', trigger: 'blur' }]
    }

    // Watch for external changes to modelValue
    watch(() => props.modelValue, (newVal) => {
      selectedScholars.value = [...newVal]
    }, { deep: true })

    // Watch for internal changes and emit
    watch(selectedScholars, (newVal) => {
      emit('update:modelValue', newVal)
    }, { deep: true })

    const handleSearch = async () => {
      if (!searchKeyword.value.trim()) {
        searchResults.value = []
        return
      }

      searching.value = true
      try {
        const response = await scholarService.searchScholars({
          keyword: searchKeyword.value,
          page: 1,
          limit: 20
        })
        if (response.code === 0) {
          searchResults.value = response.data
        } else {
          message.error(response.message || '搜索失败')
        }
      } catch (error) {
        message.error('搜索失败')
      } finally {
        searching.value = false
      }
    }

    const isScholarSelected = (scholarId) => {
      return selectedScholars.value.some(s => s.id === scholarId)
    }

    const addScholar = (scholar) => {
      if (isScholarSelected(scholar.id)) {
        message.warning('该学者已添加')
        return
      }

      selectedScholars.value.push({
        ...scholar,
        authorOrder: selectedScholars.value.length + 1,
        isCorresponding: false
      })
      message.success('添加成功')
    }

    const removeScholar = (index) => {
      selectedScholars.value.splice(index, 1)
      // Update author order
      selectedScholars.value.forEach((scholar, idx) => {
        scholar.authorOrder = idx + 1
      })
    }

    const moveUp = (index) => {
      if (index === 0) return
      const temp = selectedScholars.value[index]
      selectedScholars.value[index] = selectedScholars.value[index - 1]
      selectedScholars.value[index - 1] = temp
      // Update author order
      selectedScholars.value.forEach((scholar, idx) => {
        scholar.authorOrder = idx + 1
      })
    }

    const moveDown = (index) => {
      if (index === selectedScholars.value.length - 1) return
      const temp = selectedScholars.value[index]
      selectedScholars.value[index] = selectedScholars.value[index + 1]
      selectedScholars.value[index + 1] = temp
      // Update author order
      selectedScholars.value.forEach((scholar, idx) => {
        scholar.authorOrder = idx + 1
      })
    }

    const updateScholar = (index) => {
      // Trigger reactivity
      selectedScholars.value = [...selectedScholars.value]
    }

    const showCreateModal = () => {
      createModalVisible.value = true
    }

    const resetCreateForm = () => {
      createFormState.name = ''
      createFormState.institution = ''
      createFormState.email = ''
      createFormState.homepage = ''
      createFormState.bio = ''
    }

    const handleCreateScholar = async () => {
      try {
        await createFormRef.value.validate()
        creating.value = true

        const response = await scholarService.createScholar(createFormState)
        if (response.code === 0) {
          message.success('创建成功')
          createModalVisible.value = false
          resetCreateForm()

          // Add the newly created scholar to selected list
          addScholar(response.data)
        } else {
          message.error(response.message || '创建失败')
        }
      } catch (error) {
        console.error('表单验证失败:', error)
      } finally {
        creating.value = false
      }
    }

    return {
      searchKeyword,
      searching,
      searchResults,
      selectedScholars,
      createModalVisible,
      creating,
      createFormRef,
      createFormState,
      createRules,
      simpleImage,
      handleSearch,
      isScholarSelected,
      addScholar,
      removeScholar,
      moveUp,
      moveDown,
      updateScholar,
      showCreateModal,
      handleCreateScholar
    }
  }
})
</script>

<style scoped>
.scholar-selector {
  padding: 16px;
}

.selected-scholars,
.search-section,
.create-section {
  margin-bottom: 16px;
}

h4 {
  margin-bottom: 12px;
  font-weight: 600;
}
</style>
