<template>
  <a-card hoverable class="mentor-card" @click="goToDetail">
    <template #cover>
      <div class="mentor-avatar">
        <a-avatar :size="80" :src="mentor.avatarUrl">
          {{ mentor.name?.charAt(0) }}
        </a-avatar>
      </div>
    </template>

    <a-card-meta>
      <template #title>
        <div class="mentor-name">{{ mentor.name }}</div>
      </template>
      <template #description>
        <div class="mentor-info">
          <div class="info-item">
            <BankOutlined />
            <span>{{ mentor.institution }}</span>
          </div>
          <div class="info-item">
            <BookOutlined />
            <span>{{ mentor.researchArea }}</span>
          </div>
          <div class="info-item">
            <StarFilled style="color: #faad14" />
            <span>{{ mentor.averageRating?.toFixed(1) || '暂无评分' }}</span>
          </div>
          <div class="info-item">
            <TeamOutlined />
            <span>{{ mentor.currentStudents }}/{{ mentor.maxStudents }} 学生</span>
          </div>
        </div>
      </template>
    </a-card-meta>

    <div class="mentor-tags">
      <a-tag v-for="tag in mentor.tags?.split(',').slice(0, 3)" :key="tag" color="blue">
        {{ tag }}
      </a-tag>
    </div>

    <div class="mentor-stats">
      <span><EyeOutlined /> {{ mentor.viewCount || 0 }} 浏览</span>
      <span v-if="mentor.acceptingStudents" class="accepting">
        <CheckCircleOutlined /> 接收学生
      </span>
      <span v-else class="not-accepting">
        <CloseCircleOutlined /> 暂不接收
      </span>
    </div>
  </a-card>
</template>

<script>
import { defineComponent } from 'vue'
import { useRouter } from 'vue-router'
import {
  BankOutlined,
  BookOutlined,
  StarFilled,
  TeamOutlined,
  EyeOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'

export default defineComponent({
  name: 'MentorCard',
  components: {
    BankOutlined,
    BookOutlined,
    StarFilled,
    TeamOutlined,
    EyeOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined
  },
  props: {
    mentor: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const router = useRouter()

    const goToDetail = () => {
      router.push(`/mentors/${props.mentor.id}`)
    }

    return {
      goToDetail
    }
  }
})
</script>

<style scoped>
.mentor-card {
  cursor: pointer;
  transition: all 0.3s;
}

.mentor-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.mentor-avatar {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.mentor-name {
  font-size: 18px;
  font-weight: 600;
  text-align: center;
}

.mentor-info {
  margin-top: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  color: #666;
}

.info-item span {
  margin-left: 8px;
}

.mentor-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.mentor-stats {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}

.accepting {
  color: #52c41a;
}

.not-accepting {
  color: #ff4d4f;
}
</style>
