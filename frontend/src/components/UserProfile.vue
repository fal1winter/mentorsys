<template>
  <div class="user-profile">
    <a-card title="个人资料" :bordered="false">
      <a-spin :spinning="loading">
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
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

          <!-- Mentor-specific fields -->
          <template v-if="formState.userType === 'MENTOR' || formState.userType === 'mentor'">
            <a-divider>导师基本信息</a-divider>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="姓名">
                  <a-input v-model:value="formState.mentorProfile.name" placeholder="请输入姓名" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="职称">
                  <a-input v-model:value="formState.mentorProfile.title" placeholder="如：教授、副教授、讲师" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="所属机构">
                  <a-input v-model:value="formState.mentorProfile.institution" placeholder="请输入所属机构" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="院系">
                  <a-input v-model:value="formState.mentorProfile.department" placeholder="请输入院系" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="办公室位置">
              <a-input v-model:value="formState.mentorProfile.officeLocation" placeholder="请输入办公室位置" />
            </a-form-item>

            <a-form-item label="个人简介">
              <a-textarea v-model:value="formState.mentorProfile.bio" placeholder="请输入个人简介" :rows="3" />
            </a-form-item>

            <a-form-item label="教育背景">
              <a-textarea v-model:value="formState.mentorProfile.educationBackground" placeholder="请输入教育背景" :rows="2" />
            </a-form-item>

            <a-divider>研究方向与招生</a-divider>

            <a-form-item label="研究方向（多个用逗号分隔）">
              <a-textarea v-model:value="formState.mentorProfile.researchAreas" 
                placeholder="如：机器学习, 深度学习, 自然语言处理, 计算机视觉" :rows="2" />
            </a-form-item>

            <a-form-item label="研究关键词（多个用逗号分隔）">
              <a-input v-model:value="formState.mentorProfile.keywords" 
                placeholder="如：NLP, LLM, Transformer, BERT" />
            </a-form-item>

            <a-form-item label="组内研究方向">
              <a-textarea v-model:value="formState.mentorProfile.groupDirection" 
                placeholder="描述课题组当前的主要研究方向和项目" :rows="3" />
            </a-form-item>

            <a-form-item label="期望学生素质">
              <a-textarea v-model:value="formState.mentorProfile.expectedStudentQualities" 
                placeholder="描述您期望学生具备的能力和素质" :rows="2" />
            </a-form-item>

            <a-form-item label="指导风格">
              <a-textarea v-model:value="formState.mentorProfile.mentoringStyle" 
                placeholder="描述您的指导风格，如：每周组会、一对一指导、鼓励自主探索等" :rows="2" />
            </a-form-item>

            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item label="可招收名额">
                  <a-input-number v-model:value="formState.mentorProfile.availablePositions" :min="0" :max="20" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="最多学生数">
                  <a-input-number v-model:value="formState.mentorProfile.maxStudents" :min="0" :max="20" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item label="是否接收学生">
                  <a-switch v-model:checked="formState.mentorProfile.acceptingStudents" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item label="经费情况">
              <a-input v-model:value="formState.mentorProfile.fundingStatus" 
                placeholder="如：国家自然科学基金、企业合作项目等" />
            </a-form-item>

            <a-form-item label="合作机会">
              <a-textarea v-model:value="formState.mentorProfile.collaborationOpportunities" 
                placeholder="描述可提供的合作机会，如：企业实习、国际交流等" :rows="2" />
            </a-form-item>

            <a-divider>链接信息</a-divider>

            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item label="个人主页">
                  <a-input v-model:value="formState.mentorProfile.homepageUrl" placeholder="请输入个人主页URL" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="Google Scholar">
                  <a-input v-model:value="formState.mentorProfile.googleScholarUrl" placeholder="请输入Google Scholar链接" />
                </a-form-item>
              </a-col>
            </a-row>
          </template>

          <!-- Student-specific fields -->
          <template v-if="formState.userType === 'STUDENT' || formState.userType === 'student'">
            <a-divider>学生信息</a-divider>

            <a-form-item label="姓名">
              <a-input v-model:value="formState.studentProfile.name" placeholder="请输入姓名" />
            </a-form-item>

            <a-form-item label="当前学校">
              <a-input v-model:value="formState.studentProfile.currentInstitution" placeholder="请输入当前学校" />
            </a-form-item>

            <a-form-item label="专业">
              <a-input v-model:value="formState.studentProfile.major" placeholder="请输入专业" />
            </a-form-item>

            <a-form-item label="学位级别">
              <a-select v-model:value="formState.studentProfile.degreeLevel" placeholder="请选择学位级别">
                <a-select-option value="Bachelor">本科</a-select-option>
                <a-select-option value="Master">硕士</a-select-option>
                <a-select-option value="PhD">博士</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="毕业年份">
              <a-input-number v-model:value="formState.studentProfile.graduationYear" :min="2000" :max="2050" style="width: 100%" />
            </a-form-item>

            <a-form-item label="个人简介">
              <a-textarea v-model:value="formState.studentProfile.bio" placeholder="请输入个人简介" :rows="4" />
            </a-form-item>

            <a-form-item label="简历链接">
              <a-input v-model:value="formState.studentProfile.cvUrl" placeholder="请输入简历URL" />
            </a-form-item>
          </template>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting">
                保存修改
              </a-button>
              <a-button @click="resetForm">
                重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-spin>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import userService from '../service/userService'

export default defineComponent({
  name: 'UserProfile',
  setup() {
    const loading = ref(false)
    const submitting = ref(false)
    const formRef = ref()
    const originalData = ref({})

    const formState = reactive({
      id: null,
      username: '',
      email: '',
      phone: '',
      userType: '',
      status: null,
      createTime: '',
      lastLoginTime: '',
      // Role-specific fields
      mentorProfile: {
        name: '',
        title: '',
        institution: '',
        department: '',
        officeLocation: '',
        bio: '',
        educationBackground: '',
        researchAreas: '',
        keywords: '',
        groupDirection: '',
        expectedStudentQualities: '',
        mentoringStyle: '',
        availablePositions: 0,
        fundingStatus: '',
        collaborationOpportunities: '',
        homepageUrl: '',
        googleScholarUrl: '',
        acceptingStudents: true,
        maxStudents: 5
      },
      studentProfile: {
        name: '',
        currentInstitution: '',
        major: '',
        degreeLevel: '',
        graduationYear: null,
        bio: '',
        cvUrl: ''
      }
    })

    const userTypeText = computed(() => {
      const typeMap = {
        'ADMIN': '管理员',
        'MENTOR': '导师',
        'STUDENT': '学生'
      }
      return typeMap[formState.userType] || formState.userType
    })

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

    const fetchUserProfile = async () => {
      loading.value = true
      try {
        const response = await userService.getCompleteUserProfile()
        if (response.code === 0) {
          const data = response.data
          const user = data.user

          formState.id = user.id
          formState.username = user.username
          formState.email = user.email
          formState.phone = user.phone || ''
          formState.userType = user.userType
          formState.status = user.status
          formState.createTime = user.createTime
          formState.lastLoginTime = user.lastLoginTime || '从未登录'

          // Load role-specific data
          if (data.mentorProfile) {
            Object.assign(formState.mentorProfile, data.mentorProfile)
          }
          if (data.studentProfile) {
            Object.assign(formState.studentProfile, data.studentProfile)
          }

          // Save original data for reset
          originalData.value = JSON.parse(JSON.stringify(formState))
        } else {
          message.error(response.message || '获取用户信息失败')
        }
      } catch (error) {
        message.error('获取用户信息失败')
      } finally {
        loading.value = false
      }
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

        // Add role-specific data
        const userType = formState.userType
        if (userType === 'MENTOR' || userType === 'mentor') {
          updateData.mentorProfile = formState.mentorProfile
        } else if (userType === 'STUDENT' || userType === 'student') {
          updateData.studentProfile = formState.studentProfile
        }

        const response = await userService.updateCompleteUserProfile(updateData)
        if (response.code === 0) {
          message.success('个人资料更新成功')
          // Update original data
          originalData.value = JSON.parse(JSON.stringify(formState))
        } else {
          message.error(response.message || '更新失败')
        }
      } catch (error) {
        console.error('表单验证失败:', error)
      } finally {
        submitting.value = false
      }
    }

    const resetForm = () => {
      Object.assign(formState, originalData.value)
      formRef.value.clearValidate()
    }

    onMounted(() => {
      fetchUserProfile()
    })

    return {
      loading,
      submitting,
      formRef,
      formState,
      userTypeText,
      rules,
      handleSubmit,
      resetForm
    }
  }
})
</script>

<style scoped>
.user-profile {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}
</style>
