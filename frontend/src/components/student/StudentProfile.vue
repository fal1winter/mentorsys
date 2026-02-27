<template>
  <div class="student-profile-container">
    <a-page-header
      title="个人资料"
      @back="() => $router.push('/student/dashboard')"
    />

    <a-card>
      <a-form
        :model="formState"
        :rules="rules"
        layout="vertical"
        @finish="handleSubmit"
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="姓名" name="name">
              <a-input v-model:value="formState.name" placeholder="请输入姓名" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formState.email" placeholder="请输入邮箱" disabled />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="专业" name="major">
              <a-input v-model:value="formState.major" placeholder="请输入专业" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="学历" name="degree">
              <a-select v-model:value="formState.degree" placeholder="请选择学历">
                <a-select-option value="本科">本科</a-select-option>
                <a-select-option value="硕士">硕士</a-select-option>
                <a-select-option value="博士">博士</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="学校" name="institution">
              <a-input v-model:value="formState.institution" placeholder="请输入学校" />
            </a-form-item>
          </a-col>

          <a-col :span="12">
            <a-form-item label="入学年份" name="enrollmentYear">
              <a-input-number
                v-model:value="formState.enrollmentYear"
                :min="2000"
                :max="2030"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item label="研究兴趣" name="researchInterests">
              <a-textarea
                v-model:value="formState.researchInterests"
                :rows="4"
                placeholder="请描述您的研究兴趣"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item label="个人简介" name="bio">
              <a-textarea
                v-model:value="formState.bio"
                :rows="6"
                placeholder="请介绍您的背景和经历"
              />
            </a-form-item>
          </a-col>

          <a-col :span="24">
            <a-form-item>
              <a-space>
                <a-button type="primary" html-type="submit" :loading="loading">
                  保存
                </a-button>
                <a-button @click="() => $router.back()">
                  取消
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, reactive, ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

export default defineComponent({
  name: 'StudentProfile',
  setup() {
    const store = useStore()
    const router = useRouter()
    const loading = ref(false)

    const currentUser = computed(() => store.getters['auth/currentUser'])
    const profile = computed(() => store.getters['student/profile'])

    const formState = reactive({
      name: '',
      email: '',
      major: '',
      degree: '',
      institution: '',
      enrollmentYear: null,
      researchInterests: '',
      bio: ''
    })

    const rules = {
      name: [
        { required: true, message: '请输入姓名', trigger: 'blur' }
      ],
      major: [
        { required: true, message: '请输入专业', trigger: 'blur' }
      ],
      degree: [
        { required: true, message: '请选择学历', trigger: 'change' }
      ],
      institution: [
        { required: true, message: '请输入学校', trigger: 'blur' }
      ]
    }

    const loadProfile = async () => {
      try {
        await store.dispatch('student/fetchProfile', currentUser.value.id)
        if (profile.value) {
          Object.assign(formState, profile.value)
        }
        formState.email = currentUser.value.email
      } catch (error) {
        console.error('加载资料失败:', error)
      }
    }

    const handleSubmit = async () => {
      loading.value = true
      try {
        const profileData = {
          ...formState,
          userId: currentUser.value.id,
          id: profile.value?.id
        }

        await store.dispatch('student/updateProfile', profileData)
        message.success('保存成功')
        router.push('/student/dashboard')
      } catch (error) {
        message.error('保存失败')
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      loadProfile()
    })

    return {
      formState,
      rules,
      loading,
      handleSubmit
    }
  }
})
</script>

<style scoped>
.student-profile-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px;
}
</style>
