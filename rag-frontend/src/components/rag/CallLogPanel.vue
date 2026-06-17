<script setup lang="ts">
import type { QuestionLogResponse } from '@/types/rag'
import { computed } from 'vue'

const props = defineProps<{
  log: QuestionLogResponse | null
}>()

const formattedTime = computed(() => {
  if (!props.log?.createdAt) return ''
  return new Date(props.log.createdAt).toLocaleString('zh-CN')
})
</script>

<template>
  <el-card v-if="log" shadow="never" class="log-card">
    <template #header>
      <span class="card-title">调用日志</span>
    </template>
    <el-descriptions :column="2" size="small" border>
      <el-descriptions-item label="问题 ID">{{ log.questionId }}</el-descriptions-item>
      <el-descriptions-item label="模型">{{ log.model }}</el-descriptions-item>
      <el-descriptions-item label="创建时间" :span="2">{{ formattedTime }}</el-descriptions-item>
    </el-descriptions>
    <el-collapse style="margin-top: 12px;">
      <el-collapse-item title="完整 Prompt">
        <el-input
          :model-value="log.prompt"
          type="textarea"
          :rows="12"
          readonly
          class="log-textarea"
        />
      </el-collapse-item>
    </el-collapse>
  </el-card>
</template>

<style scoped>
.log-card {
  margin-bottom: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
.log-textarea :deep(.el-textarea__inner) {
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #303133;
}
</style>
