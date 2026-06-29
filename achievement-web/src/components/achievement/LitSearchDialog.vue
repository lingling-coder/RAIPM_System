<template>
  <el-dialog
    :model-value="visible"
    title="智能匹配结果"
    width="750px"
    top="5vh"
    @update:model-value="$emit('update:visible', $event)"
  >
    <template #header>
      <div class="dialog-header">
        <span class="dialog-title">智能匹配结果</span>
        <span class="result-count">共找到 {{ results.length }} 条候选</span>
      </div>
    </template>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>正在搜索文献数据库...</p>
    </div>

    <div v-else-if="results.length === 0" class="empty-state">
      <el-empty description="未找到匹配结果，请调整标题或作者后重试" :image-size="80" />
    </div>

    <div v-else class="result-list">
      <div
        v-for="(item, idx) in results"
        :key="item.doi || idx"
        class="result-card"
        :class="{ selected: selectedDoi === item.doi }"
        @click="selectItem(item)"
      >
        <div class="card-header">
          <span class="card-rank">#{{ idx + 1 }}</span>
          <span class="card-title">{{ item.title || '(标题未获取)' }}</span>
        </div>
        <div class="card-body">
          <div class="card-row" v-if="item.authors">
            <span class="label">作者</span>
            <span class="value">{{ item.authors }}</span>
          </div>
          <div class="card-row" v-if="item.journal">
            <span class="label">期刊</span>
            <span class="value">{{ item.journal }}</span>
          </div>
          <div class="card-row">
            <span class="label" v-if="item.publishYear">年份</span>
            <span class="value" v-if="item.publishYear">{{ item.publishYear }}</span>
            <span class="label" v-if="item.volume">卷</span>
            <span class="value" v-if="item.volume">{{ item.volume }}</span>
            <span class="label" v-if="item.issue">期</span>
            <span class="value" v-if="item.issue">{{ item.issue }}</span>
            <span class="label" v-if="item.pages">页码</span>
            <span class="value" v-if="item.pages">{{ item.pages }}</span>
          </div>
          <div class="card-row doi-row" v-if="item.doi">
            <span class="label">DOI</span>
            <el-tag size="small" type="info">{{ item.doi }}</el-tag>
          </div>
        </div>
        <div class="card-check" v-if="selectedDoi === item.doi">
          <el-icon color="#409eff" :size="22"><CircleCheckFilled /></el-icon>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button
        type="primary"
        :disabled="!selectedDoi"
        @click="confirmSelection"
      >
        确认选择并填入
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Loading, CircleCheckFilled } from '@element-plus/icons-vue'
import type { LiteratureSearchItem } from '@/api/achievement/paper'

const props = defineProps<{
  visible: boolean
  results: LiteratureSearchItem[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm', data: LiteratureSearchItem): void
}>()

const selectedDoi = ref<string | null>(null)

function selectItem(item: LiteratureSearchItem) {
  selectedDoi.value = item.doi
}

function confirmSelection() {
  const selected = props.results.find(r => r.doi === selectedDoi.value)
  if (selected) {
    emit('confirm', selected)
    emit('update:visible', false)
    selectedDoi.value = null
  } else {
    ElMessage.warning('请先选择一条匹配结果')
  }
}
</script>

<style scoped>
.dialog-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
}

.result-count {
  font-size: 12px;
  color: #909399;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 0;
  color: #909399;
  gap: 12px;
}

.empty-state {
  padding: 24px 0;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 55vh;
  overflow-y: auto;
}

.result-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px 16px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
  background: #fff;
}

.result-card:hover {
  border-color: #409eff;
  background: #ecf5ff;
}

.result-card.selected {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.card-rank {
  font-size: 12px;
  color: #909399;
  font-weight: 600;
  min-width: 24px;
  flex-shrink: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.card-body {
  padding-left: 32px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  flex-wrap: wrap;
}

.label {
  color: #909399;
  flex-shrink: 0;
}

.label::after {
  content: ':';
}

.value {
  color: #606266;
}

.doi-row {
  margin-top: 2px;
}

.card-check {
  position: absolute;
  top: 12px;
  right: 12px;
}
</style>
