<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import { deleteDocument } from '@/api/document'
import type { DocumentItem } from '@/types/document'

const props = defineProps<{
  documents: DocumentItem[]
  loading: boolean
}>()

const emit = defineEmits<{
  refresh: []
}>()

function formatTime(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function handleDelete(doc: DocumentItem) {
  try {
    await ElMessageBox.confirm(
      `确定要删除文档「${doc.title}」吗？将同时清理 MySQL 记录和 Milvus 向量数据。`,
      '确认删除',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await deleteDocument(doc.documentId)
    emit('refresh')
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span class="card-title">文档列表 ({{ documents.length }})</span>
        <el-button text size="small" @click="emit('refresh')">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </template>
    <el-table v-if="documents.length" :data="documents" stripe size="small" v-loading="loading">
      <el-table-column prop="documentId" label="ID" width="80" align="center" />
      <el-table-column prop="title" label="文档标题" min-width="200" />
      <el-table-column prop="chunkCount" label="片段数" width="100" align="center" />
      <el-table-column label="创建时间" width="180" align="center">
        <template #default="{ row }">
          {{ formatTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" text size="small" @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="暂无文档" :image-size="80" />
  </el-card>
</template>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
</style>
