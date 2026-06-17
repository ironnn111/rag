<script setup lang="ts">
const emit = defineEmits<{
  select: [question: string]
}>()

const presets = [
  '什么是 RAG？它的核心原理是什么？',
  '如何评估 RAG 系统的检索质量？',
  '向量数据库在 RAG 中的作用是什么？',
  '如何优化 RAG 系统的 chunk 切分策略？',
  'RAG 与 Fine-tuning 相比有哪些优缺点？',
]
</script>

<template>
  <div class="preset-questions">
    <span class="label">预设问题:</span>
    <div class="chips">
      <el-tag
        v-for="q in presets"
        :key="q"
        class="chip"
        effect="plain"
        @click="emit('select', q)"
      >
        {{ q }}
      </el-tag>
    </div>
  </div>
</template>

<style scoped>
.preset-questions {
  margin-bottom: 16px;
}
.label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  display: block;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  cursor: pointer;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: all 0.2s;
}
.chip:hover {
  border-color: #409eff;
  color: #409eff;
}
</style>
