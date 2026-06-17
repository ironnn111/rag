import client from './client'
import type { IngestDocumentRequest, IngestDocumentResponse, DocumentItem } from '@/types/document'

export function ingestDocument(data: IngestDocumentRequest): Promise<IngestDocumentResponse> {
  return client.post('/api/documents', data)
}

export function uploadDocument(file: File): Promise<IngestDocumentResponse> {
  const formData = new FormData()
  formData.append('file', file)
  return client.post('/api/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function listDocuments(): Promise<DocumentItem[]> {
  return client.get('/api/documents')
}

export function deleteDocument(id: number): Promise<void> {
  return client.delete(`/api/documents/${id}`)
}
