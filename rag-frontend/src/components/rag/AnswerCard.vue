<script setup lang="ts">
import { ElMessage } from 'element-plus'

const props = defineProps<{
  answer: string
}>()

function copyAnswer() {
  if (!props.answer) return
  navigator.clipboard.writeText(props.answer).then(() => {
    ElMessage.success('已复制到剪贴板')
  })
}
</script>

<template>
  <el-card shadow="never" class="answer-card">
    <template #header>
      <div class="card-header">
        <span class="card-title">AI 回答</span>
        <el-button v-if="answer" text size="small" @click="copyAnswer">
          <el-icon><CopyDocument /></el-icon>
          复制
        </el-button>
      </div>
    </template>
    <div v-if="answer" class="answer-content">{{ answer }}</div>
    <el-empty v-else description="暂无回答" :image-size="60" />
  </el-card>
</template>

<style scoped>
.answer-card {
  margin-bottom: 16px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
.answer-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
}
</style>
