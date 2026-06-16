<template>
  <div class="attachment-uploader">
    <!-- Upload area -->
    <el-upload
      ref="uploadRef"
      :auto-upload="false"
      :multiple="true"
      :accept="acceptTypes"
      :before-upload="beforeUpload"
      :http-request="handleUpload"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-progress="handleProgress"
      :file-list="fileList"
      drag
      list-type="text"
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将文件拖拽到此处，或<em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF/Word/Excel/图片/压缩包，单文件不超过 50MB，不限制数量
        </div>
      </template>
    </el-upload>

    <!-- Uploaded files table -->
    <el-table v-if="uploadedFiles.length > 0" :data="uploadedFiles" style="margin-top: 12px">
      <el-table-column prop="originalName" label="文件名" min-width="200" />
      <el-table-column prop="fileSizeDisplay" label="大小" width="100" />
      <el-table-column prop="uploadTime" label="上传时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button type="primary" size="small" link @click="handleDownload(scope.row)">
            下载
          </el-button>
          <el-button
            v-if="showDelete"
            type="danger"
            size="small"
            link
            @click="handleDelete(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import * as attachmentApi from '@/api/attachment'

const MAX_FILE_SIZE = 50 * 1024 * 1024 // 50MB
const acceptTypes = '.pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.zip,.rar'

interface UploadedFile {
  id: number
  originalName: string
  fileSize: number
  fileSizeDisplay: string
  uploadTime?: string
}

const props = withDefaults(defineProps<{
  achievementType: string
  achievementId?: number
  isClassified?: boolean
  showDelete?: boolean
}>(), {
  isClassified: false,
  showDelete: true,
})

const uploadedFiles = ref<UploadedFile[]>([])
const fileList = ref<any[]>([])
const uploadRef = ref<any>(null)

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function beforeUpload(file: File): boolean {
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error(`文件 "${file.name}" 超过 50MB 限制`)
    return false
  }
  return true
}

async function handleUpload(options: any) {
  const { file, onSuccess, onError } = options
  try {
    if (!props.achievementId) {
      ElMessage.warning('请先保存草稿后再上传附件')
      return
    }
    const res: any = await attachmentApi.upload(file, props.achievementType, props.achievementId)
    const uploaded: UploadedFile = {
      id: res?.data || Date.now(),
      originalName: file.name,
      fileSize: file.size,
      fileSizeDisplay: formatFileSize(file.size),
      uploadTime: new Date().toLocaleString(),
    }
    uploadedFiles.value.push(uploaded)
    onSuccess(res, file)
    ElMessage.success(`"${file.name}" 上传成功`)
  } catch (err: any) {
    ElMessage.error(`"${file.name}" 上传失败: ${err?.message || '请重试'}`)
    onError(err)
  }
}

async function handleDownload(row: UploadedFile) {
  try {
    const res: any = await attachmentApi.download(row.id)
    const blob = res instanceof Blob ? res : new Blob([res])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.originalName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
  } catch (err: any) {
    if (err?.response?.status === 403) {
      ElMessage.error('无权限下载该附件')
    } else {
      ElMessage.error('附件下载失败')
    }
  }
}

function handleProgress(event: any, file: any) {
  // Progress handled by el-upload's built-in progress bar
}

function handleSuccess(response: any, file: any) {
  // File added already in handleUpload
}

function handleError(err: any, file: any) {
  // Error shown already in handleUpload
}

async function handleDelete(row: UploadedFile) {
  try {
    await ElMessageBox.confirm('确认删除该附件？此操作不可撤销。', '确认删除', {
      type: 'warning',
    })
    await attachmentApi.deleteAttachment(row.id)
    uploadedFiles.value = uploadedFiles.value.filter((f) => f.id !== row.id)
    ElMessage.success('附件已删除')
  } catch {
    // Cancelled
  }
}
</script>

<style scoped>
.attachment-uploader {
  width: 100%;
}
</style>
