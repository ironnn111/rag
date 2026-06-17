import { ref } from 'vue'
import { askRag, getQuestionLog } from '@/api/rag'
import type { AskResponse, QuestionLogResponse, TokenUsage, RetrievalHit } from '@/types/rag'

export function useRagAsk() {
  const loading = ref(false)
  const answer = ref('')
  const tokenUsage = ref<TokenUsage | null>(null)
  const retrievalResults = ref<RetrievalHit[]>([])
  const error = ref('')
  const questionId = ref<number | null>(null)
  const callLog = ref<QuestionLogResponse | null>(null)

  async function ask(question: string, topK?: number, similarityThreshold?: number) {
    loading.value = true
    error.value = ''
    answer.value = ''
    tokenUsage.value = null
    retrievalResults.value = []
    questionId.value = null
    callLog.value = null

    try {
      const res: AskResponse = await askRag({ question, topK, similarityThreshold })
      answer.value = res.answer
      tokenUsage.value = res.tokenUsage
      retrievalResults.value = res.retrievalResults
      questionId.value = res.questionId
      await fetchLog(res.questionId)
    } catch (e: any) {
      error.value = e?.response?.data?.message || e.message || '请求失败'
    } finally {
      loading.value = false
    }
  }

  async function fetchLog(id: number) {
    try {
      callLog.value = await getQuestionLog(id)
    } catch {
      // log fetch failure is non-critical
    }
  }

  return { loading, answer, tokenUsage, retrievalResults, error, questionId, callLog, ask }
}
