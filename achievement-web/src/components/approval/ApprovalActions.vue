<template>
  <div class="approval-actions">
    <!-- Section Header -->
    <h3 class="section-header">审批操作</h3>

    <!-- Pass / Reject Button Row -->
    <div class="action-row">
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        :disabled="loading"
        class="action-btn action-btn-half"
        @click="handlePass"
      >
        通过
      </el-button>

      <el-button
        type="danger"
        size="large"
        :loading="loading"
        :disabled="loading"
        class="action-btn action-btn-half"
        @click="showRejectInput = !showRejectInput"
      >
        退回
      </el-button>
    </div>

    <!-- Reject Reason Input (shown when reject clicked) -->
    <div v-if="showRejectInput" class="reject-section">
      <el-divider />

      <!-- Quick-select reasons -->
      <el-select
        v-model="rejectReason"
        filterable
        allow-create
        default-first-option
        placeholder="选择或输入退回原因"
        class="full-width"
      >
        <el-option
          v-for="reason in filteredRejectReasons"
          :key="reason"
          :label="reason"
          :value="reason"
        />
      </el-select>

      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请填写退回原因（必填）"
        class="reject-textarea"
      />

      <el-button
        type="danger"
        :loading="loading"
        :disabled="!rejectReason.trim()"
        class="action-btn"
        @click="handleReject"
      >
        确认退回
      </el-button>
    </div>

    <!-- Archive Input (admin only, PENDING_ADMIN_ARCHIVE) -->
    <div v-if="isAdmin && status === 'PENDING_ADMIN_ARCHIVE'" class="archive-section">
      <el-divider />
      <p class="archive-hint">归档操作</p>
      <el-input
        v-model="archiveNo"
        placeholder="输入归档号（留空自动生成）"
        class="archive-input"
      />
      <el-button
        type="primary"
        :loading="loading"
        :disabled="loading"
        class="action-btn"
        @click="handleArchive"
      >
        确认归档
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'

const props = defineProps<{
  achievementType: string
  achievementId: number
  status: string
  isAdmin: boolean
  loading: boolean
}>()

const emit = defineEmits<{
  pass: [archiveNo?: string]
  reject: [reason: string]
  archive: [archiveNo: string]
}>()

const showRejectInput = ref(false)
const rejectReason = ref('')
const archiveNo = ref('')

// Reject reason quick-select options per UI-SPEC §5.5
const allRejectReasons = [
  '信息填写不完整，请补充必填字段',
  '附件缺失，请上传相关证明文件',
  '成果不属于本部门管理范围',
  '格式不符合要求，请参考模板',
  '涉密等级分类有误，请调整',
  'DOI/申请号格式不正确',
  '论文未正式发表或录用',
  '专利法律状态信息不准确',
  '软著著作权人信息有误',
  '其他原因（请手动填写）',
]

const filteredRejectReasons = computed(() => {
  if (props.achievementType === 'paper') {
    return allRejectReasons.filter(r => !r.includes('专利') && !r.includes('软著'))
  }
  if (props.achievementType === 'patent') {
    return allRejectReasons.filter(r => !r.includes('论文') && !r.includes('软著'))
  }
  if (props.achievementType === 'copyright') {
    return allRejectReasons.filter(r => !r.includes('论文') && !r.includes('专利'))
  }
  return allRejectReasons.filter(r => !r.includes('论文') && !r.includes('专利') && !r.includes('软著'))
})

async function handlePass() {
  try {
    await ElMessageBox.confirm(
      '确认通过该成果审批？通过后将进入下一审批节点。',
      '确认通过',
      { type: 'info', confirmButtonText: '确认通过', cancelButtonText: '取消' }
    )
    emit('pass')
  } catch {
    // User cancelled
  }
}

async function handleReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('退回原因不能为空')
    return
  }
  try {
    await ElMessageBox.confirm(
      '请填写退回原因，退回后将通知提交人修改后重新提交。',
      '确认退回',
      { type: 'info', confirmButtonText: '确认退回', cancelButtonText: '取消' }
    )
    emit('reject', rejectReason.value.trim())
    showRejectInput.value = false
    rejectReason.value = ''
  } catch {
    // User cancelled
  }
}

async function handleArchive() {
  const archiveVal = archiveNo.value.trim()
  try {
    await ElMessageBox.confirm(
      archiveVal
        ? `确认使用编号 "${archiveVal}" 归档？`
        : '确认归档？系统将自动生成成果编号。',
      '分配归档号',
      { type: 'info', confirmButtonText: '确认归档', cancelButtonText: '取消' }
    )
    emit('archive', archiveVal || '')
  } catch {
    // User cancelled
  }
}
</script>

<style scoped>
.approval-actions {
  padding: 16px;
}

.section-header {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 16px 0;
}

.action-row {
  display: flex;
  gap: 12px;
}

.action-btn {
  margin-bottom: 12px;
}

.action-btn-half {
  flex: 1;
}

.full-width {
  width: 100%;
  margin-bottom: 12px;
}

.reject-textarea {
  margin-bottom: 12px;
}

.reject-section,
.archive-section {
  margin-top: 8px;
}

.archive-hint {
  font-size: 14px;
  color: #606266;
  margin-bottom: 8px;
}

.archive-input {
  margin-bottom: 12px;
}
</style>
