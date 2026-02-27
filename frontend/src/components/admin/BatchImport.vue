<template>
  <div class="batch-import">
    <div class="page-header">
      <div class="header-content">
        <h2>📥 批量导入用户</h2>
        <p class="header-desc">通过上传 Excel 表格批量创建导师或学生账号</p>
      </div>
      <a-button @click="$router.push('/admin/users')">返回用户管理</a-button>
    </div>

    <div class="import-cards">
      <!-- 导师导入 -->
      <a-card class="import-card">
        <template #title>
          <div class="card-title">👨‍🏫 批量导入导师</div>
        </template>
        <div class="card-body">
          <p class="card-desc">上传包含导师信息的 Excel 文件，系统将自动创建用户账号和导师资料。</p>
          <div class="template-section">
            <span>第一步：下载模板文件，按格式填写数据</span>
            <a-button type="link" @click="downloadTemplate('mentor')" :loading="downloadingMentor">
              📄 下载导师模板
            </a-button>
          </div>
          <div class="upload-section">
            <span>第二步：上传填写好的 Excel 文件</span>
            <a-upload
              :before-upload="(file) => handleBeforeUpload(file, 'mentor')"
              :show-upload-list="false"
              accept=".xlsx,.xls"
            >
              <a-button type="primary" :loading="importingMentor">
                <UploadOutlined /> 选择文件并导入
              </a-button>
            </a-upload>
          </div>
          <div class="field-info">
            <p>必填字段：用户名、密码、邮箱、姓名</p>
            <p>可选字段：职称、机构、院系、研究方向、关键词、简介等</p>
          </div>
        </div>
      </a-card>

      <!-- 学生导入 -->
      <a-card class="import-card">
        <template #title>
          <div class="card-title">👨‍🎓 批量导入学生</div>
        </template>
        <div class="card-body">
          <p class="card-desc">上传包含学生信息的 Excel 文件，系统将自动创建用户账号和学生资料。</p>
          <div class="template-section">
            <span>第一步：下载模板文件，按格式填写数据</span>
            <a-button type="link" @click="downloadTemplate('student')" :loading="downloadingStudent">
              📄 下载学生模板
            </a-button>
          </div>
          <div class="upload-section">
            <span>第二步：上传填写好的 Excel 文件</span>
            <a-upload
              :before-upload="(file) => handleBeforeUpload(file, 'student')"
              :show-upload-list="false"
              accept=".xlsx,.xls"
            >
              <a-button type="primary" :loading="importingStudent">
                <UploadOutlined /> 选择文件并导入
              </a-button>
            </a-upload>
          </div>
          <div class="field-info">
            <p>必填字段：用户名、密码、邮箱、姓名</p>
            <p>可选字段：学校、专业、学位、GPA、研究兴趣、编程技能等</p>
          </div>
        </div>
      </a-card>
    </div>

    <!-- 导入结果 -->
    <a-card v-if="importResult" class="result-card" :title="'导入结果'">
      <div class="result-summary">
        <a-statistic title="总计" :value="importResult.total" class="stat-item" />
        <a-statistic title="成功" :value="importResult.success" class="stat-item stat-success"
                     :value-style="{ color: '#52c41a' }" />
        <a-statistic title="失败" :value="importResult.fail" class="stat-item stat-fail"
                     :value-style="{ color: importResult.fail > 0 ? '#ff4d4f' : '#999' }" />
      </div>

      <a-table
        :columns="resultColumns"
        :data-source="importResult.details"
        :pagination="{ pageSize: 20 }"
        row-key="row"
        size="small"
        class="result-table"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'success' ? 'green' : 'red'">
              {{ record.status === 'success' ? '成功' : '失败' }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import { defineComponent, ref } from 'vue'
import { message } from 'ant-design-vue'
import { UploadOutlined } from '@ant-design/icons-vue'
import userService from '../../service/userService'

export default defineComponent({
  name: 'BatchImport',
  components: { UploadOutlined },
  setup() {
    const downloadingMentor = ref(false)
    const downloadingStudent = ref(false)
    const importingMentor = ref(false)
    const importingStudent = ref(false)
    const importResult = ref(null)

    const resultColumns = [
      { title: '行号', dataIndex: 'row', key: 'row', width: 70 },
      { title: '状态', key: 'status', width: 80 },
      { title: '用户名', dataIndex: 'username', key: 'username' },
      { title: '姓名', dataIndex: 'name', key: 'name' },
      { title: '错误信息', dataIndex: 'error', key: 'error' }
    ]

    const downloadTemplate = async (type) => {
      const loadingRef = type === 'mentor' ? downloadingMentor : downloadingStudent
      loadingRef.value = true
      try {
        const response = type === 'mentor'
          ? await userService.downloadMentorTemplate()
          : await userService.downloadStudentTemplate()
        // response 是 blob（因为 axios interceptor 返回 response.data）
        const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = type === 'mentor' ? 'mentor_import_template.xlsx' : 'student_import_template.xlsx'
        a.click()
        window.URL.revokeObjectURL(url)
        message.success('模板下载成功')
      } catch (error) {
        message.error('下载模板失败')
      } finally {
        loadingRef.value = false
      }
    }

    const handleBeforeUpload = async (file, type) => {
      const loadingRef = type === 'mentor' ? importingMentor : importingStudent
      loadingRef.value = true
      importResult.value = null
      try {
        const response = type === 'mentor'
          ? await userService.importMentors(file)
          : await userService.importStudents(file)
        if (response.code === 0) {
          importResult.value = response.data
          const { success, fail } = response.data
          if (fail === 0) {
            message.success(`导入完成，全部 ${success} 条成功`)
          } else {
            message.warning(`导入完成：${success} 条成功，${fail} 条失败`)
          }
        } else {
          message.error(response.message || '导入失败')
        }
      } catch (error) {
        message.error('导入失败，请检查文件格式')
      } finally {
        loadingRef.value = false
      }
      return false // 阻止默认上传
    }

    return {
      downloadingMentor, downloadingStudent,
      importingMentor, importingStudent,
      importResult, resultColumns,
      downloadTemplate, handleBeforeUpload
    }
  }
})
</script>

<style scoped>
.batch-import { max-width: 1200px; margin: 0 auto; padding: 24px; }
.page-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px; padding: 24px 32px; margin-bottom: 24px;
  color: #fff; display: flex; justify-content: space-between; align-items: center;
}
.page-header h2 { margin: 0; color: #fff; font-size: 24px; }
.header-desc { margin: 4px 0 0; opacity: 0.85; }
.import-cards { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
.import-card { border-radius: 12px; }
.card-title { font-size: 18px; font-weight: 600; }
.card-desc { color: #666; margin-bottom: 16px; }
.template-section, .upload-section {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px; background: #f8f9ff; border-radius: 8px; margin-bottom: 12px;
}
.template-section span, .upload-section span { font-size: 14px; color: #555; }
.field-info { padding: 12px 16px; background: #fffbe6; border-radius: 8px; border: 1px solid #ffe58f; }
.field-info p { margin: 4px 0; font-size: 13px; color: #8c6d1f; }
.result-card { border-radius: 12px; }
.result-summary { display: flex; gap: 40px; margin-bottom: 20px; padding: 16px; background: #fafafa; border-radius: 8px; }
.stat-item { text-align: center; }
.result-table { margin-top: 12px; }
@media (max-width: 768px) { .import-cards { grid-template-columns: 1fr; } }
</style>
