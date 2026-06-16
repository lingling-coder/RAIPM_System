<template>
  <el-dialog
    v-model="dialogVisible"
    title="批量缴费"
    width="650px"
    :close-on-click-modal="false"
    :before-close="handleClose"
  >
    <!-- Step 1: Confirm selected fee records -->
    <div v-if="step === 1">
      <el-alert
        type="info"
        :title="`已选择 ${selectedRecords.length} 条待缴费记录`"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-table :data="selectedRecords" border stripe max-height="360">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column label="费用类型" width="100">
          <template #default="scope">
            <span>{{ feeTypeLabel(scope.row.feeType) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联成果" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.ownerName || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="120">
          <template #default="scope">
            {{ scope.row.dueDate || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110" align="right">
          <template #default="scope">
            ¥{{ scope.row.amount ? scope.row.amount.toFixed(2) : '0.00' }}
          </template>
        </el-table-column>
      </el-table>

      <!-- Total summary -->
      <div class="total-summary">
        共 <strong>{{ selectedRecords.length }}</strong> 条记录，
        合计 <strong>¥{{ totalAmount.toFixed(2) }}</strong>
      </div>

      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="generating" @click="generateSlips">
          生成缴费单
        </el-button>
      </div>
    </div>

    <!-- Step 2: Fill payment info -->
    <div v-if="step === 2">
      <el-alert
        type="success"
        title="缴费单已生成"
        show-icon
        :closable="false"
        style="margin-bottom: 16px"
      />

      <el-form label-width="120px">
        <el-form-item label="缴费单号">
          <div class="slip-numbers">
            <el-tag
              v-for="(no, idx) in slipNumbers"
              :key="idx"
              type="primary"
              style="margin-right: 6px; margin-bottom: 4px"
            >
              {{ no }}
            </el-tag>
          </div>
        </el-form-item>

        <el-form-item label="缴费日期" required>
          <el-date-picker
            v-model="paidDate"
            type="date"
            placeholder="选择缴费日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="凭证号" required>
          <el-input
            v-model="voucherNo"
            placeholder="请输入缴费凭证号"
            maxlength="50"
          />
        </el-form-item>
      </el-form>

      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">
          确认缴费
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as feeApi from '@/api/fee/feeRecord'
import type { FeeRecordVO } from '@/api/fee/feeRecord'

const props = defineProps<{
  visible: boolean
  selectedIds: number[]
  selectedRecords: FeeRecordVO[]
}>()

const emit = defineEmits<{
  'update:visible': [visible: boolean]
  success: [paidCount: number]
}>()

// ── State ──────────────────────────────────────────────────────────

const dialogVisible = ref(props.visible)
const step = ref(1)
const generating = ref(false)
const paying = ref(false)
const slipNumbers = ref<string[]>([])
const paidDate = ref(new Date().toISOString().slice(0, 10))
const voucherNo = ref('')

// Watch visible prop to sync dialog state
watch(() => props.visible, (val) => {
  dialogVisible.value = val
  if (val) {
    // Reset state when dialog opens
    step.value = 1
    slipNumbers.value = []
    paidDate.value = new Date().toISOString().slice(0, 10)
    voucherNo.value = ''
  }
})

// ── Computed ───────────────────────────────────────────────────────

const totalAmount = computed(() => {
  return props.selectedRecords.reduce((sum, r) => sum + (r.amount || 0), 0)
})

// ── Fee type label helper ──────────────────────────────────────────

function feeTypeLabel(type: string): string {
  const map: Record<string, string> = {
    annual_fee: '专利年费',
    registration_fee: '登记费',
    maintenance_fee: '维护费',
    other: '其他',
  }
  return map[type] || type
}

// ── Step 1: Generate slips ────────────────────────────────────────

async function generateSlips() {
  generating.value = true
  try {
    const res: any = await feeApi.batchGenerateSlips(props.selectedIds)
    slipNumbers.value = res?.data || []
    step.value = 2
    ElMessage.success(`已生成 ${slipNumbers.value.length} 份缴费单`)
  } catch {
    // Error handled by API interceptor
  } finally {
    generating.value = false
  }
}

// ── Step 2: Confirm payment ────────────────────────────────────────

async function confirmPay() {
  // Validate voucher number
  if (!voucherNo.value.trim()) {
    ElMessage.warning('请输入缴费凭证号')
    return
  }

  paying.value = true
  try {
    const res: any = await feeApi.batchPay({
      ids: props.selectedIds,
      paidDate: paidDate.value,
      voucherNo: voucherNo.value.trim(),
    })
    const count = res?.data || props.selectedIds.length
    ElMessage.success(`已成功标记 ${count} 条记录为已缴费`)
    emit('success', count)
  } catch {
    // Error handled by API interceptor
  } finally {
    paying.value = false
  }
}

// ── Dialog lifecycle ──────────────────────────────────────────────

function handleClose() {
  dialogVisible.value = false
  emit('update:visible', false)
}
</script>

<style scoped>
.total-summary {
  text-align: right;
  margin: 12px 0;
  font-size: 14px;
  color: #606266;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.slip-numbers {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
</style>
