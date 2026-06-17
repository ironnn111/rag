export interface AskRequest {
  question: string
  topK?: number
  similarityThreshold?: number
}

export interface TokenUsage {
  inputTokens: number
  outputTokens: number
  totalTokens: number
}

export interface RetrievalHit {
  rank: number
  score: number
  documentId: number
  title: string
  chunkIndex: number
  content: string
}

export interface AskResponse {
  questionId: number
  question: string
  answer: string
  tokenUsage: TokenUsage
  retrievalResults: RetrievalHit[]
}

export interface QuestionLogResponse {
  questionId: number
  question: string
  answer: string
  model: string
  prompt: string
  tokenUsage: TokenUsage
  createdAt: string
  retrievalResults: RetrievalHit[]
}
