<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  send: [question: string]
}>()

const input = ref('')
const loading = defineModel<boolean>('loading', { default: false })

function handleSend() {
  const q = input.value.trim()
  if (!q || loading.value) return
  emit('send', q)
  input.value = ''
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="chat-input">
    <el-input
      v-model="input"
      type="textarea"
      :rows="3"
      placeholder="请输入您的问题，按 Enter 发送..."
      :disabled="loading"
      @keydown="handleKeydown"
    />
    <el-button
      type="primary"
      :loading="loading"
      :disabled="!input.trim()"
      class="send-btn"
      @click="handleSend"
    >
      <el-icon v-if="!loading"><Promotion /></el-icon>
      <span>{{ loading ? '思考中...' : '发送' }}</span>
    </el-button>
  </div>
</template>

<style scoped>
.chat-input {
  position: relative;
}
.send-btn {
  position: absolute;
  right: 8px;
  bottom: 8px;
}
</style>
