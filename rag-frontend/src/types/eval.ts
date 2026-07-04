export interface EvalScoreResponse {
  evalId: number
  questionId: number
  faithfulnessScore: number
  faithfulnessReason: string
  relevancyScore: number
  relevancyReason: string
  createdAt: string
}

export interface QuestionSummary {
  questionId: number
  question: string
  createdAt: string
}
