<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ingestDocument, uploadDocument } from '@/api/document'
import type { IngestDocumentResponse } from '@/types/document'

const emit = defineEmits<{
  uploaded: [response: IngestDocumentResponse]
}>()

const activeTab = ref('json')
const loading = ref(false)

// JSON 模式
const title = ref('')
const content = ref('')

async function handleJsonSubmit() {
  if (!title.value.trim() || !content.value.trim()) {
    ElMessage.warning('请填写文档标题和内容')
    return
  }
  loading.value = true
  try {
    const res = await ingestDocument({ title: title.value.trim(), content: content.value.trim() })
    ElMessage.success(`入库成功，切分为 ${res.chunkCount} 个片段`)
    emit('uploaded', res)
    title.value = ''
    content.value = ''
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

// 文件上传模式
const fileList = ref<any[]>([])

async function handleFileChange(file: any) {
  loading.value = true
  try {
    const res = await uploadDocument(file.raw)
    ElMessage.success(`"${res.title}" 入库成功，切分为 ${res.chunkCount} 个片段`)
    emit('uploaded', res)
    fileList.value = []
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
  return false // prevent default upload
}
</script>

<template>
  <el-card shadow="never" class="upload-card">
    <template #header>
      <span class="card-title">文档入库</span>
    </template>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="JSON 文本" name="json">
        <el-form label-width="80px" size="default">
          <el-form-item label="文档标题">
            <el-input v-model="title" placeholder="请输入文档标题" />
          </el-form-item>
          <el-form-item label="文档内容">
            <el-input
              v-model="content"
              type="textarea"
              :rows="8"
              placeholder="请输入文档内容..."
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" @click="handleJsonSubmit">
              提交入库
            </el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="Word 文件" name="file">
        <el-upload
          v-model:file-list="fileList"
          drag
          :limit="1"
          :auto-upload="false"
          accept=".docx,.doc"
          :http-request="handleFileChange"
          :on-change="handleFileChange"
        >
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">将 Word 文件拖到此处，或点击上传</div>
          <template #tip>
            <div class="upload-tip">支持 .docx / .doc 格式</div>
          </template>
        </el-upload>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<style scoped>
.upload-card {
  margin-bottom: 20px;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
}
.upload-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 8px;
}
.upload-text {
  font-size: 14px;
  color: #606266;
}
.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}
</style>
