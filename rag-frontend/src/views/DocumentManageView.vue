<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listDocuments } from '@/api/document'
import type { DocumentItem } from '@/types/document'
import DocumentUpload from '@/components/document/DocumentUpload.vue'
import DocumentList from '@/components/document/DocumentList.vue'

const documents = ref<DocumentItem[]>([])
const loading = ref(false)

async function fetchDocuments() {
  loading.value = true
  try {
    documents.value = await listDocuments()
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function handleUploaded() {
  fetchDocuments()
}

onMounted(() => {
  fetchDocuments()
})
</script>

<template>
  <div class="doc-manage">
    <DocumentUpload @uploaded="handleUploaded" />
    <DocumentList :documents="documents" :loading="loading" @refresh="fetchDocuments" />
  </div>
</template>

<style scoped>
.doc-manage {
  max-width: 1000px;
  margin: 0 auto;
}
</style>
