<script setup lang="ts">
import type { TokenUsage } from '@/types/rag'

defineProps<{
  usage: TokenUsage | null
}>()
</script>

<template>
  <el-card shadow="never" class="token-card">
    <template #header>
      <span class="card-title">Token 统计</span>
    </template>
    <div v-if="usage" class="token-stats">
      <div class="stat-item">
        <div class="stat-value">{{ usage.inputTokens?.toLocaleString() ?? '-' }}</div>
        <div class="stat-label">输入 Token</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ usage.outputTokens?.toLocaleString() ?? '-' }}</div>
        <div class="stat-label">输出 Token</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ usage.totalTokens?.toLocaleString() ?? '-' }}</div>
        <div class="stat-label">总计 Token</div>
      </div>
    </div>
    <el-empty v-else description="暂无数据" :image-size="60" />
  </el-card>
</template>

<style scoped>
.token-card {
  margin-bottom: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
.token-stats {
  display: flex;
  gap: 32px;
}
.stat-item {
  text-align: center;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
</style>
