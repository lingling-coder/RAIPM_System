<template>
  <el-dialog
    :model-value="visible"
    title="DOI 补全结果预览"
    width="600px"
    @update:model-value="$emit('update:visible', $event)"
  >
    <el-descriptions :column="1" border>
      <el-descriptions-item label="DOI">
        <el-tag>{{ data?.doi }}</el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="标题">
        <el-tag v-if="data?.title" type="success">已匹配</el-tag>
        <el-tag v-else type="danger">未匹配</el-tag>
        {{ data?.title || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="作者">
        <el-tag v-if="data?.authors" type="success">已匹配</el-tag>
        <el-tag v-else type="danger">未匹配</el-tag>
        {{ data?.authors || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="期刊">
        <el-tag v-if="data?.journal" type="success">已匹配</el-tag>
        <el-tag v-else type="danger">未匹配</el-tag>
        {{ data?.journal || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="卷号">
        <el-tag v-if="data?.volume" type="success">已匹配</el-tag>
        <el-tag v-else type="info">无</el-tag>
        {{ data?.volume || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="期号">
        <el-tag v-if="data?.issue" type="success">已匹配</el-tag>
        <el-tag v-else type="info">无</el-tag>
        {{ data?.issue || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="页码">
        <el-tag v-if="data?.pages" type="success">已匹配</el-tag>
        <el-tag v-else type="info">无</el-tag>
        {{ data?.pages || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="发表年份">
        <el-tag v-if="data?.publishYear" type="success">已匹配</el-tag>
        <el-tag v-else type="danger">未匹配</el-tag>
        {{ data?.publishYear || '—' }}
      </el-descriptions-item>
      <el-descriptions-item label="摘要">
        <el-tag v-if="data?.abstractText" type="success">已匹配</el-tag>
        <el-tag v-else type="info">无</el-tag>
        <div v-if="data?.abstractText" class="abstract-preview">
          {{ data.abstractText.substring(0, 200) }}{{ data.abstractText.length > 200 ? '...' : '' }}
        </div>
      </el-descriptions-item>
    </el-descriptions>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" @click="confirm">确认填入</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { DoiLookupResult } from '@/api/achievement/paper'

defineProps<{
  visible: boolean
  data: DoiLookupResult | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'confirm'): void
}>()

function confirm() {
  emit('confirm')
  emit('update:visible', false)
}
</script>

<style scoped>
.abstract-preview {
  max-height: 80px;
  overflow: hidden;
  font-size: 12px;
  color: #606266;
  margin-top: 4px;
}
</style>
