export interface IngestDocumentRequest {
  title: string
  content: string
}

export interface IngestDocumentResponse {
  documentId: number
  title: string
  chunkCount: number
}

export interface DocumentItem {
  documentId: number
  title: string
  chunkCount: number
  createdAt: string
}
