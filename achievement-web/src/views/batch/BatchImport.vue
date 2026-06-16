<template>
  <div class="batch-import">
    <h2 class="page-title">批量导入</h2>

    <!-- Step 1: Download Template -->
    <el-card class="import-step-card" shadow="never">
      <template #header>
        <div class="step-header">
          <el-tag type="primary" class="step-number">1</el-tag>
          <span class="step-title">下载模板</span>
        </div>
      </template>
      <div class="step-body">
        <p class="step-desc">模板包含论文/专利/软著所有字段，通过"类型"列区分</p>
        <el-button type="primary" :loading="downloading" @click="handleDownloadTemplate">
          <el-icon class="el-icon--left"><Download /></el-icon>
          下载Excel导入模板
        </el-button>
      </div>
    </el-card>

    <!-- Step 2: Upload File -->
    <el-card class="import-step-card" shadow="never">
      <template #header>
        <div class="step-header">
          <el-tag type="primary" class="step-number">2</el-tag>
          <span class="step-title">上传文件</span>
        </div>
      </template>
      <div class="step-body" v-loading="importing" element-loading-text="正在导入，请稍候...">
        <el-upload
          ref="uploadRef"
          :auto-upload="false"
          accept=".xlsx,.xls"
          :before-upload="beforeUpload"
          :http-request="handleImport"
          drag
          :show-file-list="false"
          :disabled="importing"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将Excel文件拖拽到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 .xlsx / .xls 格式
            </div>
          </template>
        </el-upload>
      </div>
    </el-card>

    <!-- Step 3: Import Result (conditional) -->
    <el-card v-if="importResult" class="import-step-card" shadow="never">
      <template #header>
        <div class="step-header">
          <el-tag type="primary" class="step-number">3</el-tag>
          <span class="step-title">导入结果</span>
        </div>
      </template>
      <div class="step-body">
        <el-result
          :icon="importResult.hasErrors() ? 'warning' : 'success'"
          :title="resultTitle"
          :sub-title="resultSubtitle"
        >
          <template #extra>
            <div class="result-stats">
              <div class="stat-row">
                <span class="stat-label">共</span>
                <span class="stat-value">{{ importResult.totalRows }}</span>
                <span class="stat-label">行</span>
              </div>
              <div class="stat-row success">
                <span class="stat-label">成功导入</span>
                <span class="stat-value">{{ importResult.successRows }}</span>
                <span class="stat-label">行</span>
              </div>
              <div v-if="importResult.errorRows > 0" class="stat-row error">
                <span class="stat-label">失败</span>
                <span class="stat-value">{{ importResult.errorRows }}</span>
                <span class="stat-label">行</span>
              </div>
              <div v-if="importResult.skippedRows > 0" class="stat-row skipped">
                <span class="stat-label">跳过重复</span>
                <span class="stat-value">{{ importResult.skippedRows }}</span>
                <span class="stat-label">行</span>
              </div>
            </div>
            <el-button
              v-if="importResult.errorRows > 0 && importResult.importRecordId"
              type="primary"
              plain
              @click="handleDownloadErrorReport"
            >
              <el-icon class="el-icon--left"><Download /></el-icon>
              下载错误报告
            </el-button>
          </template>
        </el-result>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import { downloadTemplate, importFile, getErrorReport } from '@/api/batchImport'

interface BatchImportResult {
  totalRows: number
  successRows: number
  errorRows: number
  skippedRows: number
  errors?: Array<{ rowIndex: number; type: string; reasons: string[] }>
  errorReportPath?: string
  importRecordId?: number
}

const downloading = ref(false)
const importing = ref(false)
const importResult = ref<BatchImportResult | null>(null)
const uploadRef = ref<any>(null)

const resultTitle = computed(() => {
  if (!importResult.value) return ''
  if (importResult.value.totalRows === 0) return '文件为空'
  if (importResult.value.errorRows === 0 && importResult.value.skippedRows === 0) return '全部导入成功'
  if (importResult.value.successRows === 0) return '导入失败'
  return '部分行导入失败'
})

const resultSubtitle = computed(() => {
  if (!importResult.value) return ''
  if (importResult.value.errorRows > 0 && importResult.value.successRows > 0) {
    return `共导入 ${importResult.value.successRows} 行，${importResult.value.errorRows} 行失败，${importResult.value.skippedRows} 行跳过`
  }
  if (importResult.value.skippedRows > 0 && importResult.value.successRows > 0) {
    return `成功导入 ${importResult.value.successRows} 行，${importResult.value.skippedRows} 行重复已跳过`
  }
  return ''
})

async function handleDownloadTemplate() {
  downloading.value = true
  try {
    const res: any = await downloadTemplate()
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '科研成果导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (err) {
    ElMessage.error('模板下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

function beforeUpload(file: File): boolean {
  const validExtensions = ['.xlsx', '.xls']
  const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase()
  if (!validExtensions.includes(ext)) {
    ElMessage.error('请上传 .xlsx 或 .xls 格式的文件')
    return false
  }
  return true
}

async function handleImport(options: any) {
  const { file, onSuccess, onError } = options
  importing.value = true
  importResult.value = null
  try {
    const res: any = await importFile(file)
    if (res && res.data) {
      importResult.value = res.data
      onSuccess(res, file)
      ElMessage.success('文件导入完成')
    } else {
      throw new Error('导入失败')
    }
  } catch (err: any) {
    importResult.value = {
      totalRows: 0,
      successRows: 0,
      errorRows: 0,
      skippedRows: 0,
    }
    onError(err)
    ElMessage.error(err?.message || '文件导入失败')
  } finally {
    importing.value = false
  }
}

async function handleDownloadErrorReport() {
  if (!importResult.value?.importRecordId) {
    ElMessage.warning('无错误报告可下载')
    return
  }
  try {
    const res: any = await getErrorReport(importResult.value.importRecordId)
    const blob = res instanceof Blob ? res : new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `导入错误报告_${importResult.value.importRecordId}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (err) {
    ElMessage.error('错误报告下载失败')
  }
}
</script>

<style scoped>
.batch-import {
  max-width: 720px;
  margin: 0 auto;
  padding: 20px 0;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
  color: var(--el-text-color-primary);
}

.import-step-card {
  margin-bottom: 20px;
  border: 1px solid var(--el-border-color-base);
  border-radius: 8px;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-number {
  font-size: 16px;
  font-weight: 700;
  border-radius: 50%;
  min-width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.step-body {
  padding: 8px 0;
}

.step-desc {
  font-size: 14px;
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
  line-height: 1.5;
}

.result-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
}

.stat-row {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}

.stat-row .stat-label {
  color: var(--el-text-color-secondary);
}

.stat-row .stat-value {
  font-size: 18px;
  font-weight: 700;
  min-width: 24px;
  text-align: center;
}

.stat-row.success .stat-value {
  color: var(--el-color-success);
}

.stat-row.error .stat-value {
  color: var(--el-color-danger);
}

.stat-row.skipped .stat-value {
  color: var(--el-color-warning);
}
</style>
