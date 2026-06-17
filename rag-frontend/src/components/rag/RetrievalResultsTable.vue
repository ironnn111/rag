<script setup lang="ts">
import type { RetrievalHit } from '@/types/rag'

const props = defineProps<{
  results: RetrievalHit[]
}>()

const scoreType = (score: number) => {
  if (score >= 0.8) return 'success'
  if (score >= 0.6) return 'warning'
  return 'danger'
}
</script>

<template>
  <el-card shadow="never" class="retrieval-card">
    <template #header>
      <span class="card-title">检索结果 ({{ results.length }} 条)</span>
    </template>
    <el-table v-if="results.length" :data="results" stripe size="small">
      <el-table-column prop="rank" label="#" width="50" align="center" />
      <el-table-column label="相似度" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="scoreType(row.score)" size="small" effect="dark">
            {{ row.score?.toFixed(4) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="来源文档" min-width="160" />
      <el-table-column label="片段" min-width="180">
        <template #default="{ row }">
          <el-text class="chunk-text" truncated>{{ row.content }}</el-text>
        </template>
      </el-table-column>
      <el-table-column prop="chunkIndex" label="分块索引" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">#{{ row.chunkIndex }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="暂无检索结果" :image-size="60" />
  </el-card>
</template>

<style scoped>
.retrieval-card {
  margin-bottom: 16px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
.chunk-text {
  max-width: 100%;
  cursor: default;
}
</style>
