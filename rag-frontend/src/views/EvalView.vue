<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listEvalQuestions, evaluateQuestion, evaluateRagas, getEvalScore } from '@/api/eval'
import type { QuestionSummary, EvalScoreResponse } from '@/types/eval'
import { ElMessage } from 'element-plus'

const questions = ref<QuestionSummary[]>([])
const loading = ref(false)
const evaluatingId = ref<number | null>(null)
const ragasEvaluatingId = ref<number | null>(null)
const evalResults = ref<Map<number, EvalScoreResponse | null>>(new Map())

async function loadQuestions() {
  loading.value = true
  try {
    questions.value = await listEvalQuestions()
    for (const q of questions.value) {
      try {
        const score = await getEvalScore(q.questionId)
        evalResults.value.set(q.questionId, score)
      } catch {
        evalResults.value.set(q.questionId, null)
      }
    }
  } finally {
    loading.value = false
  }
}

async function runEvaluate(questionId: number) {
  evaluatingId.value = questionId
  try {
    const result = await evaluateQuestion(questionId)
    evalResults.value.set(questionId, result)
    ElMessage.success('评估完成')
  } catch (e: any) {
    ElMessage.error(e?.message || '评估失败')
  } finally {
    evaluatingId.value = null
  }
}

async function runRagas(questionId: number) {
  ragasEvaluatingId.value = questionId
  try {
    const result = await evaluateRagas(questionId)
    evalResults.value.set(questionId, result)
    ElMessage.success('RAGAS 评估完成')
  } catch (e: any) {
    ElMessage.error(e?.message || 'RAGAS 评估失败')
  } finally {
    ragasEvaluatingId.value = null
  }
}

function scoreColor(score: number): string {
  if (score >= 4) return 'success'
  if (score >= 3) return 'warning'
  return 'danger'
}

function scoreLabel(score: number): string {
  const labels: Record<number, string> = {
    1: '差', 2: '较差', 3: '一般', 4: '良好', 5: '优秀'
  }
  return labels[score] || ''
}

onMounted(loadQuestions)
</script>

<template>
  <div class="eval-page">
    <div class="eval-header">
      <h2>问答质量评估</h2>
      <p class="eval-desc">基于 LLM-as-Judge 对 RAG 问答结果进行忠实度（Faithfulness）和相关性（Relevancy）自动评估</p>
    </div>

    <el-table
      :data="questions"
      v-loading="loading"
      empty-text="暂无可评估的问题"
      row-key="questionId"
    >
      <el-table-column prop="questionId" label="ID" width="70" />
      <el-table-column prop="question" label="问题" min-width="300" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="提问时间" width="180">
        <template #default="{ row }">
          {{ new Date(row.createdAt).toLocaleString('zh-CN') }}
        </template>
      </el-table-column>
      <el-table-column label="评估操作" width="200" fixed="right">
        <template #default="{ row }">
          <div class="eval-actions">
            <el-button
              type="primary"
              size="small"
              :loading="evaluatingId === row.questionId"
              @click="runEvaluate(row.questionId)"
            >
              LLM 评估
            </el-button>
            <el-button
              type="success"
              size="small"
              :loading="ragasEvaluatingId === row.questionId"
              @click="runRagas(row.questionId)"
            >
              RAGAS
            </el-button>
          </div>
        </template>
      </el-table-column>
      <el-table-column type="expand">
        <template #default="{ row }">
          <div class="eval-expand" v-if="evalResults.get(row.questionId)">
            <div class="score-cards">
              <el-card shadow="hover" class="score-card">
                <template #header>
                  <div class="card-header">
                    <span>忠实度 Faithfulness</span>
                    <el-tag
                      :type="scoreColor(evalResults.get(row.questionId)!.faithfulnessScore) as any"
                      size="large"
                    >
                      {{ evalResults.get(row.questionId)!.faithfulnessScore }} 分
                      {{ scoreLabel(evalResults.get(row.questionId)!.faithfulnessScore) }}
                    </el-tag>
                  </div>
                </template>
                <p>{{ evalResults.get(row.questionId)!.faithfulnessReason }}</p>
              </el-card>

              <el-card shadow="hover" class="score-card">
                <template #header>
                  <div class="card-header">
                    <span>相关性 Relevancy</span>
                    <el-tag
                      :type="scoreColor(evalResults.get(row.questionId)!.relevancyScore) as any"
                      size="large"
                    >
                      {{ evalResults.get(row.questionId)!.relevancyScore }} 分
                      {{ scoreLabel(evalResults.get(row.questionId)!.relevancyScore) }}
                    </el-tag>
                  </div>
                </template>
                <p>{{ evalResults.get(row.questionId)!.relevancyReason }}</p>
              </el-card>
            </div>
          </div>
          <div class="eval-expand" v-else-if="evaluatingId === row.questionId">
            <el-skeleton :rows="3" animated />
          </div>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.eval-actions {
  display: flex;
  gap: 6px;
}

.eval-page {
  max-width: 1000px;
  margin: 0 auto;
}

.eval-header {
  margin-bottom: 24px;
}

.eval-header h2 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.eval-desc {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.eval-expand {
  padding: 16px 24px;
}

.score-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.score-card .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-card p {
  margin: 0;
  color: #606266;
  line-height: 1.7;
  font-size: 14px;
}

@media (max-width: 768px) {
  .score-cards {
    grid-template-columns: 1fr;
  }
}
</style>
