<script setup lang="ts">
import { ref } from 'vue'
import { useRagAsk } from '@/composables/useRagAsk'
import PresetQuestions from '@/components/rag/PresetQuestions.vue'
import ParameterPanel from '@/components/rag/ParameterPanel.vue'
import ChatInput from '@/components/rag/ChatInput.vue'
import AnswerCard from '@/components/rag/AnswerCard.vue'
import RetrievalResultsTable from '@/components/rag/RetrievalResultsTable.vue'
import TokenUsageCard from '@/components/rag/TokenUsageCard.vue'
import CallLogPanel from '@/components/rag/CallLogPanel.vue'

const { loading, answer, tokenUsage, retrievalResults, error, callLog, ask } = useRagAsk()

const topK = ref(5)
const threshold = ref(0.6)

function handleSend(question: string) {
  ask(question, topK.value, threshold.value)
}

function handlePresetSelect(question: string) {
  ask(question, topK.value, threshold.value)
}
</script>

<template>
  <div class="rag-qa">
    <div class="qa-input-area">
      <PresetQuestions @select="handlePresetSelect" />
      <ParameterPanel v-model:topK="topK" v-model:threshold="threshold" />
      <ChatInput :loading="loading" @send="handleSend" />
    </div>

    <el-alert v-if="error" type="error" :title="error" show-icon closable class="error-alert" />

    <div v-if="answer || loading" class="qa-results">
      <AnswerCard :answer="answer" />
      <RetrievalResultsTable :results="retrievalResults" />
      <el-row :gutter="16">
        <el-col :span="12">
          <TokenUsageCard :usage="tokenUsage" />
        </el-col>
      </el-row>
      <CallLogPanel :log="callLog" />
    </div>
  </div>
</template>

<style scoped>
.rag-qa {
  max-width: 1000px;
  margin: 0 auto;
}
.qa-input-area {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.qa-results {
  margin-top: 20px;
}
.error-alert {
  margin-bottom: 16px;
}
</style>
