import client from './client'
import type { EvalScoreResponse, QuestionSummary } from '@/types/eval'

export function listEvalQuestions(): Promise<QuestionSummary[]> {
  return client.get('/api/eval/questions')
}

export function evaluateQuestion(questionId: number): Promise<EvalScoreResponse> {
  return client.post(`/api/eval/${questionId}`)
}

export function getEvalScore(questionId: number): Promise<EvalScoreResponse | null> {
  return client.get(`/api/eval/${questionId}`)
}
