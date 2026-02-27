<template>
  <div class="mentor-rating-create">
    <a-button 
      type="primary"
      @click="toggleForm"
      style="margin-bottom: 16px"
    >
      {{ showForm ? '收起评价' : '发表评价' }}
    </a-button>

    <transition name="fade">
      <div v-show="showForm" class="form-wrapper">
        <a-form :model="form" ref="formRef" :rules="rules">
          <a-form-item name="rating">
            <div class="rating-container">
              <span class="rating-number">{{ form.rating.toFixed(1) }}</span>
              <a-rate 
                v-model:value="integerRating" 
                allow-half 
                class="big-stars"
                @change="handleIntegerChange"
              />
              <a-rate 
                v-model:value="decimalRating" 
                allow-half 
                class="small-stars"
                @change="handleDecimalChange"
              />
            </div>
          </a-form-item>
          
          <a-form-item name="comment">
            <div class="editor-container">
              <Toolbar
                :editor="editorRef"
                :defaultConfig="toolbarConfig"
                mode="default"
                class="toolbar"
              />
              <Editor
                v-model="form.comment"
                :defaultConfig="editorConfig"
                mode="default"
                class="editor"
                @onCreated="handleCreated"
              />
            </div>
          </a-form-item>

          <a-form-item>
            <a-button 
              type="primary" 
              @click="handleSubmit"
              :loading="submitting"
            >
              提交评价
            </a-button>
          </a-form-item>
        </a-form>
      </div>
    </transition>
  </div>
</template>

<script>
import { defineComponent, ref, shallowRef, onBeforeUnmount, reactive, computed } from 'vue'
import { message } from 'ant-design-vue'
import { useStore } from 'vuex'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import ratingService from '../../service/ratingService'

export default defineComponent({
  name: 'MentorRatingCreate',
  components: { Editor, Toolbar },
  props: {
    mentorId: {
      type: Number,
      required: true
    }
  },
  emits: ['refresh'],
  setup(props, { emit }) {
    const store = useStore()
    const currentUser = computed(() => store.getters['auth/currentUser'])
    const editorRef = shallowRef()
    const formRef = ref()
    const showForm = ref(false)
    const submitting = ref(false)
    const integerRating = ref(0)
    const decimalRating = ref(0)

    const form = reactive({
      rating: 0,
      comment: ''
    })

    const rules = {
      rating: [{ required: true, type: 'number', min: 0.1, message: '请选择评分' }],
      comment: [{ required: true, message: '请输入评价内容' }]
    }

    const toolbarConfig = {
      excludeKeys: ['group-video', 'fullScreen']
    }

    const editorConfig = {
      placeholder: '请输入您对导师的评价...',
      MENU_CONF: {}
    }

    const handleCreated = (editor) => {
      editorRef.value = editor
    }

    const toggleForm = () => {
      showForm.value = !showForm.value
    }

    const handleIntegerChange = (val) => {
      const intScore = val * 2
      decimalRating.value = 0
      updateTotalRating(intScore, 0)
    }

    const handleDecimalChange = (val) => {
      if (val === 5) {
        if (integerRating.value < 5) {
          integerRating.value += 0.5
          decimalRating.value = 0
          const currentIntScore = integerRating.value * 2
          updateTotalRating(currentIntScore, 0)
          return
        } else {
          val = 0
          decimalRating.value = 0
          message.warning('满分10分时不能再添加小数评分')
        }
      }

      if (val > 4.5) {
        val = 4.5
        decimalRating.value = 4.5
      }

      if (integerRating.value === 5) {
        val = 0
        decimalRating.value = 0
        message.warning('满分10分时不能再添加小数评分')
      }

      const decimalScore = val / 5
      const currentIntScore = integerRating.value * 2
      updateTotalRating(currentIntScore, decimalScore)
    }

    const updateTotalRating = (intPart, decPart) => {
      let total = intPart + decPart
      if (total > 10) total = 10
      form.rating = Number(total.toFixed(1))
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()

        if (!currentUser.value || !currentUser.value.id) {
          message.error('请先登录')
          return
        }

        submitting.value = true

        const response = await ratingService.createRating({
          mentorId: props.mentorId,
          studentId: currentUser.value.id,  // 使用当前用户ID
          rating: form.rating,
          comment: form.comment
        })

        if (response.code === 0) {
          message.success('评价提交成功')
          form.rating = 0
          form.comment = ''
          integerRating.value = 0
          decimalRating.value = 0
          showForm.value = false
          emit('refresh')
        } else {
          message.error(response.message || '评价提交失败')
        }
      } catch (error) {
        console.error('评价提交失败:', error)
        message.error('评价提交失败')
      } finally {
        submitting.value = false
      }
    }

    onBeforeUnmount(() => {
      const editor = editorRef.value
      if (editor) {
        editor.destroy()
      }
    })

    return {
      editorRef,
      formRef,
      showForm,
      submitting,
      integerRating,
      decimalRating,
      form,
      rules,
      toolbarConfig,
      editorConfig,
      handleCreated,
      toggleForm,
      handleIntegerChange,
      handleDecimalChange,
      handleSubmit
    }
  }
})
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.rating-container {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.rating-number {
  font-size: 24px;
  font-weight: bold;
  color: #ff6b35;
  min-width: 50px;
}

:deep(.big-stars) {
  font-size: 24px;
}

:deep(.small-stars) {
  font-size: 14px;
  margin-top: 6px;
}

:deep(.ant-rate-star-zero .ant-rate-star-first .anticon-star),
:deep(.ant-rate-star-zero .ant-rate-star-second .anticon-star) {
  color: #666666 !important;
  opacity: 0.7;
}

:deep(.ant-rate-star-full .ant-rate-star-second .anticon-star) {
  color: #ff6b35 !important;
  opacity: 1;
  filter: drop-shadow(0 0 2px rgba(255, 107, 53, 0.4));
}

:deep(.ant-rate-star-half .ant-rate-star-first .anticon-star) {
  color: #ff6b35 !important;
  opacity: 1;
  filter: drop-shadow(0 0 2px rgba(255, 107, 53, 0.4));
}

:deep(.ant-rate-star-half .ant-rate-star-second .anticon-star) {
  color: #666666 !important;
  opacity: 0.7;
}

.editor-container {
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}

.toolbar {
  border-bottom: 1px solid #e8e8e8;
}

.editor {
  min-height: 200px;
}
</style>
