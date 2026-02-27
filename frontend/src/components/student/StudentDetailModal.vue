<template>
  <div class="student-detail" v-if="student">
    <a-descriptions :column="2" bordered size="small">
      <a-descriptions-item label="姓名" :span="1">{{ student.name }}</a-descriptions-item>
      <a-descriptions-item label="邮箱" :span="1">{{ student.email || '-' }}</a-descriptions-item>
      <a-descriptions-item label="学校" :span="1">{{ student.currentInstitution || '-' }}</a-descriptions-item>
      <a-descriptions-item label="专业" :span="1">{{ student.major || '-' }}</a-descriptions-item>
      <a-descriptions-item label="学位" :span="1">{{ student.degreeLevel || '-' }}</a-descriptions-item>
      <a-descriptions-item label="预计毕业" :span="1">{{ student.graduationYear || '-' }}</a-descriptions-item>
      <a-descriptions-item label="GPA" :span="1">{{ student.gpa || '-' }}</a-descriptions-item>
      <a-descriptions-item label="发表论文数" :span="1">{{ student.publicationsCount || 0 }}</a-descriptions-item>
    </a-descriptions>

    <a-divider>研究信息</a-divider>

    <div class="info-section" v-if="student.researchInterests">
      <h4>研究兴趣</h4>
      <div class="tags">
        <a-tag v-for="interest in parseJsonArray(student.researchInterests)" :key="interest" color="blue">
          {{ interest }}
        </a-tag>
      </div>
    </div>

    <div class="info-section" v-if="student.expectedResearchDirection">
      <h4>期望研究方向</h4>
      <p>{{ student.expectedResearchDirection }}</p>
    </div>

    <div class="info-section" v-if="student.personalAbilities">
      <h4>个人能力</h4>
      <p>{{ student.personalAbilities }}</p>
    </div>

    <div class="info-section" v-if="student.programmingSkills">
      <h4>编程技能</h4>
      <div class="tags">
        <a-tag v-for="skill in parseJsonArray(student.programmingSkills)" :key="skill" color="green">
          {{ skill }}
        </a-tag>
      </div>
    </div>

    <div class="info-section" v-if="student.projectExperience">
      <h4>项目经验</h4>
      <p>{{ student.projectExperience }}</p>
    </div>

    <div class="info-section" v-if="student.bio">
      <h4>个人简介</h4>
      <p>{{ student.bio }}</p>
    </div>

    <div class="info-section" v-if="student.preferredMentorStyle">
      <h4>期望导师风格</h4>
      <p>{{ student.preferredMentorStyle }}</p>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue'

export default defineComponent({
  name: 'StudentDetailModal',
  props: {
    student: {
      type: Object,
      default: null
    }
  },
  setup() {
    const parseJsonArray = (jsonStr) => {
      if (!jsonStr) return []
      try {
        const parsed = JSON.parse(jsonStr)
        return Array.isArray(parsed) ? parsed : []
      } catch {
        return []
      }
    }

    return { parseJsonArray }
  }
})
</script>

<style scoped>
.student-detail {
  padding: 16px 0;
}

.info-section {
  margin-bottom: 16px;
}

.info-section h4 {
  color: #1890ff;
  margin-bottom: 8px;
  font-size: 14px;
}

.info-section p {
  color: #666;
  line-height: 1.6;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
