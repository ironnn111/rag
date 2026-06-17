import client from './client'
import type { AskRequest, AskResponse, QuestionLogResponse } from '@/types/rag'

export function askRag(params: AskRequest): Promise<AskResponse> {
  return client.post('/api/rag/ask', params)
}

export function getQuestionLog(questionId: number): Promise<QuestionLogResponse> {
  return client.get(`/api/rag/questions/${questionId}`)
}
