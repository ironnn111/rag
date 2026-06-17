<script setup lang="ts">
const topK = defineModel<number>('topK', { default: 5 })
const threshold = defineModel<number>('threshold', { default: 0.6 })
</script>

<template>
  <el-card shadow="never" class="param-panel">
    <div class="param-row">
      <span class="param-label">检索数量 (topK)</span>
      <el-input-number
        v-model="topK"
        :min="1"
        :max="20"
        size="small"
        controls-position="right"
      />
    </div>
    <div class="param-row">
      <span class="param-label">相似度阈值</span>
      <el-slider
        v-model="threshold"
        :min="0.1"
        :max="1.0"
        :step="0.1"
        :format-tooltip="(v: number) => v.toFixed(1)"
        style="flex: 1; margin-left: 12px;"
      />
      <span class="param-value">{{ threshold.toFixed(1) }}</span>
    </div>
  </el-card>
</template>

<style scoped>
.param-panel {
  margin-bottom: 16px;
}
.param-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
.param-row:last-child {
  margin-bottom: 0;
}
.param-label {
  width: 140px;
  font-size: 13px;
  color: #606266;
  flex-shrink: 0;
}
.param-value {
  width: 36px;
  text-align: center;
  font-size: 13px;
  color: #409eff;
  font-weight: 600;
  margin-left: 8px;
}
</style>
