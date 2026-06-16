<template>
  <div class="fee-ledger">
    <h2 class="page-title">费用台账</h2>

    <!-- Quick filter tags (D-04) -->
    <div class="quick-tags">
      <el-tag
        v-for="tag in quickTags"
        :key="tag.key"
        :type="activeQuickTag === tag.key ? 'primary' : 'info'"
        :effect="activeQuickTag === tag.key ? 'dark' : 'plain'"
        style="cursor: pointer; margin-right: 8px"
        @click="setQuickFilter(tag.key)"
      >
        {{ tag.label }}
      </el-tag>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <el-select v-model="filters.feeType" placeholder="费用类型" clearable style="width: 140px" @change="fetchData">
        <el-option label="专利年费" value="annual_fee" />
        <el-option label="登记费" value="registration_fee" />
        <el-option label="维护费" value="maintenance_fee" />
        <el-option label="其他" value="other" />
      </el-select>

      <el-select v-model="filters.status" placeholder="缴费状态" clearable style="width: 130px" @change="fetchData">
        <el-option label="待缴费" value="pending" />
        <el-option label="已缴费" value="paid" />
        <el-option label="已暂停" value="paused" />
      </el-select>

      <el-select v-model="filters.fundingSource" placeholder="经费来源" clearable style="width: 150px" @change="fetchData">
        <el-option label="纵向科研经费" value="vertical" />
        <el-option label="横向科研经费" value="horizontal" />
        <el-option label="院配套" value="institute" />
        <el-option label="自筹" value="self" />
      </el-select>

      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="截止日期起"
        end-placeholder="截止日期止"
        style="width: 240px"
        @change="onDateRangeChange"
      />

      <el-input
        v-model="filters.keyword"
        placeholder="搜索成果名称"
        clearable
        style="width: 180px"
        @keyup.enter="fetchData"
      />

      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>

      <el-button text type="primary" @click="showAdvanced = !showAdvanced">
        {{ showAdvanced ? '收起高级筛选' : '高级筛选' }}
        <el-icon><ArrowUp v-if="showAdvanced" /><ArrowDown v-else /></el-icon>
      </el-button>

      <el-button
        type="success"
        :disabled="selectedRecords.length === 0"
        @click="openBatchPayDialog"
      >
        生成缴费单
      </el-button>
    </div>

    <!-- Advanced filters (collapsible) -->
    <el-collapse-transition>
      <div v-if="showAdvanced" class="advanced-filters">
        <el-select v-model="filters.ownerType" placeholder="成果类型" clearable style="width: 140px" @change="fetchData">
          <el-option label="专利" value="patent" />
          <el-option label="软件著作权" value="copyright" />
        </el-select>
      </div>
    </el-collapse-transition>

    <!-- Table (D-03) -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%; margin-top: 16px"
      @row-click="goToDetail"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column label="费用类型" width="110">
        <template #default="scope">
          <el-tag :type="feeTagType(scope.row.feeType)" size="small">
            {{ scope.row.feeTypeLabel || scope.row.feeType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联成果" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" :underline="false" @click.stop="goToAchievement(asFee(scope.row))">
            {{ (asFee(scope.row)).ownerName || '—' }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="截止日期" width="120">
        <template #default="scope">
          <span :style="{ color: dueDateColor(asFee(scope.row)) }">
            {{ asFee(scope.row).dueDate || '—' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="预警" width="120">
        <template #default="scope">
          <el-tag
            v-if="getAlertTag(asFee(scope.row))"
            :type="getAlertTag(asFee(scope.row)).type"
            :effect="getAlertTag(asFee(scope.row)).effect || 'dark'"
            size="small"
          >
            {{ getAlertTag(asFee(scope.row)).text }}
          </el-tag>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="scope">
          ¥{{ scope.row.amount ? scope.row.amount.toFixed(2) : '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="实缴金额" width="120" align="right">
        <template #default="scope">
          <span v-if="scope.row.paidAmount">¥{{ scope.row.paidAmount.toFixed(2) }}</span>
          <span v-else>—</span>
        </template>
      </el-table-column>
      <el-table-column label="缴费日期" width="120">
        <template #default="scope">
          {{ scope.row.paidDate || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="经费来源" width="120">
        <template #default="scope">
          {{ fundSourceLabel(scope.row.fundingSource) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ scope.row.statusLabel || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" link @click.stop="viewDetail(asFee(scope.row))">查看</el-button>
          <el-button size="small" type="warning" link @click.stop="openEdit(asFee(scope.row))">编辑</el-button>
          <el-button
            size="small"
            type="danger"
            link
            :disabled="asFee(scope.row).status !== 'paused'"
            @click.stop="handleDelete(asFee(scope.row))"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Empty state -->
    <el-empty v-if="!loading && tableData.length === 0" description="暂无费用记录" />

    <!-- Pagination -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>

    <!-- Batch payment dialog (02-04 Task 3) -->
    <BatchPayDialog
      v-model:visible="batchPayDialogVisible"
      :selected-ids="selectedIds"
      :selected-records="selectedRecords"
      @success="onBatchPaySuccess"
    />

    <!-- Edit dialog (D-17: dueDate locked, amount/fundingSource editable) -->
    <el-dialog v-model="editDialogVisible" title="编辑费用记录" width="500px">
      <el-form v-if="editForm" :model="editForm" label-width="110px">
        <el-form-item label="费用类型">
          <el-tag :type="feeTagType(editForm.feeType)">
            {{ editForm.feeTypeLabel || editForm.feeType }}
          </el-tag>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="editForm.dueDate" type="date" disabled style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="editForm.amount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="实缴金额">
          <el-input-number v-model="editForm.paidAmount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="缴费凭证号">
          <el-input v-model="editForm.voucherNo" placeholder="请输入凭证号" />
        </el-form-item>
        <el-form-item label="经费来源">
          <el-select v-model="editForm.fundingSource" placeholder="选择经费来源" style="width: 100%">
            <el-option label="纵向科研经费" value="vertical" />
            <el-option label="横向科研经费" value="horizontal" />
            <el-option label="院配套" value="institute" />
            <el-option label="自筹" value="self" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="待缴费" value="pending" />
            <el-option label="已缴费" value="paid" />
            <el-option label="已暂停" value="paused" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowUp, ArrowDown } from '@element-plus/icons-vue'
import * as feeApi from '@/api/fee/feeRecord'
import type { FeeRecordVO } from '@/api/fee/feeRecord'
import BatchPayDialog from '@/views/fee/components/BatchPayDialog.vue'

/** TagType used by Element Plus el-tag */
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const router = useRouter()

// ── State ──────────────────────────────────────────────────────────
const loading = ref(false)
const editLoading = ref(false)
const tableData = ref<FeeRecordVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const showAdvanced = ref(false)
const activeQuickTag = ref('all')
const dateRange = ref<any>(null)
const editDialogVisible = ref(false)
const editForm = ref<any>(null)
const currentEditId = ref<number | null>(null)

const quickTags = [
  { key: 'all', label: '全部' },
  { key: 'impending', label: '即将逾期' },
  { key: 'overdue', label: '已逾期' },
  { key: 'dueThisMonth', label: '本月需缴费' },
]

const filters = reactive({
  feeType: null as string | null,
  status: null as string | null,
  fundingSource: null as string | null,
  keyword: '',
  dueDateFrom: null as string | null,
  dueDateTo: null as string | null,
})

// ── Batch selection (02-04 Task 3) ────────────────────────────────
const selectedRecords = ref<FeeRecordVO[]>([])
const batchPayDialogVisible = ref(false)

const selectedIds = computed(() => {
  return selectedRecords.value.map(r => r.id)
})

function openBatchPayDialog() {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择待缴费的费用记录')
    return
  }
  batchPayDialogVisible.value = true
}

function onBatchPaySuccess(paidCount: number) {
  batchPayDialogVisible.value = false
  selectedRecords.value = []
  fetchData()
}

// ── Quick filter logic ────────────────────────────────────────────

function setQuickFilter(key: string) {
  activeQuickTag.value = key
  // Build query params from quick filter
  const today = new Date()
  const formatLocalDate = (d: Date) => {
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const d2 = String(d.getDate()).padStart(2, '0')
    return y + '-' + m + '-' + d2
  }

  switch (key) {
    case 'all':
      filters.status = null
      filters.dueDateFrom = null
      filters.dueDateTo = null
      break
    case 'impending':
      filters.status = 'pending'
      filters.dueDateFrom = formatLocalDate(today)
      {
        const future = new Date(today)
        future.setDate(future.getDate() + 30)
        filters.dueDateTo = formatLocalDate(future)
      }
      break
    case 'overdue':
      filters.status = 'pending'
      filters.dueDateFrom = null
      filters.dueDateTo = formatLocalDate(today)
      break
    case 'dueThisMonth':
      filters.status = 'pending'
      filters.dueDateFrom = formatLocalDate(new Date(today.getFullYear(), today.getMonth(), 1))
      filters.dueDateTo = formatLocalDate(new Date(today.getFullYear(), today.getMonth() + 1, 0))
      break
  }

  page.value = 1
  fetchData()
}

// ── Date range handler ────────────────────────────────────────────

function onDateRangeChange(val: any) {
  if (val) {
    filters.dueDateFrom = formatLocalDate(val[0])
    filters.dueDateTo = formatLocalDate(val[1])
  } else {
    filters.dueDateFrom = null
    filters.dueDateTo = null
  }
  page.value = 1
  fetchData()
}

// ── Fetch data ────────────────────────────────────────────────────

async function fetchData() {
  loading.value = true

  try {
    const res: any = await feeApi.getPage({
      page: page.value,
      size: pageSize.value,
      status: filters.status || undefined,
      feeType: filters.feeType || undefined,
      fundingSource: filters.fundingSource || undefined,
      keyword: filters.keyword || undefined,
      dueDateFrom: filters.dueDateFrom || undefined,
      dueDateTo: filters.dueDateTo || undefined,
    })

    if (res?.data) {
      tableData.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// ── Filters reset ─────────────────────────────────────────────────

function resetFilters() {
  filters.feeType = null
  filters.status = null
  filters.fundingSource = null
  filters.keyword = ''
  filters.dueDateFrom = null
  filters.dueDateTo = null
  filters.ownerType = null
  dateRange.value = null
  activeQuickTag.value = 'all'
  page.value = 1
  fetchData()
}

// ── Selection handler (02-04 Task 3) ──────────────────────────────

function onSelectionChange(selection: any[]) {
  selectedRecords.value = selection as FeeRecordVO[]
}

// ── Helpers ───────────────────────────────────────────────────────
/** Cast table row to FeeRecordVO for type safety in el-table scoped slots */
function asFee(row: any): FeeRecordVO { return row as FeeRecordVO }

// ── Navigation ────────────────────────────────────────────────────

function goToDetail(row: FeeRecordVO) {
  router.push(`/fee/detail/${row.id}`)
}

function viewDetail(row: FeeRecordVO) {
  router.push(`/fee/detail/${row.id}`)
}

function goToAchievement(row: FeeRecordVO) {
  if (row.ownerType === 'patent') {
    router.push(`/achievement/detail/${row.ownerId}`)
  } else if (row.ownerType === 'copyright') {
    router.push(`/achievement/detail/${row.ownerId}`)
  }
}

// ── Delete (only paused) ──────────────────────────────────────────

async function handleDelete(row: FeeRecordVO) {
  if (row.status !== 'paused') {
    ElMessage.warning('仅已暂停的费用记录可以删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认删除该费用记录？此操作不可撤销。',
      '确认删除',
      { type: 'warning' }
    )
    await feeApi.remove(row.id)
    ElMessage.success('费用记录已删除')
    fetchData()
  } catch {
    // cancelled
  }
}


// ── Timezone-safe date helpers (CR-04) ──────────────────────────────

/** Parse YYYY-MM-DD string as local date components, no timezone offset */
function parseLocalDate(str: string): Date | null {
  if (!str) return null
  const parts = str.split('-')
  const y = parseInt(parts[0], 10)
  const m = parseInt(parts[1], 10)
  const d = parseInt(parts[2], 10)
  if (isNaN(y) || isNaN(m) || isNaN(d)) return null
  return new Date(y, m - 1, d)
}

/** Format local Date to YYYY-MM-DD string, no timezone offset */
function formatLocalDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return y + '-' + m + '-' + d
}

/** Get today's date as YYYY-MM-DD, no timezone offset */
function todayLocal(): string {
  const d = new Date()
  return formatLocalDate(d)
}

// ── Edit dialog ───────────────────────────────────────────────────

function openEdit(row: FeeRecordVO) {
  currentEditId.value = row.id
  editForm.value = {
    feeType: row.feeType,
    feeTypeLabel: row.feeTypeLabel,
    dueDate: row.dueDate ? parseLocalDate(row.dueDate) : null,
    amount: row.amount,
    paidAmount: row.paidAmount,
    voucherNo: row.voucherNo,
    fundingSource: row.fundingSource,
    status: row.status,
  }
  editDialogVisible.value = true
}

async function saveEdit() {
  if (!currentEditId.value || !editForm.value) return

  editLoading.value = true
  try {
    await feeApi.update(currentEditId.value, {
      feeType: editForm.value.feeType,
      amount: editForm.value.amount,
      paidAmount: editForm.value.paidAmount,
      dueDate: editForm.value.dueDate ? formatLocalDate(editForm.value.dueDate) : '',
      voucherNo: editForm.value.voucherNo,
      fundingSource: editForm.value.fundingSource,
      status: editForm.value.status,
    })
    ElMessage.success('费用记录已更新')
    editDialogVisible.value = false
    fetchData()
  } catch {
    // error handled by API interceptor
  } finally {
    editLoading.value = false
  }
}

// ── Display helpers ───────────────────────────────────────────────

function getAlertTag(row: FeeRecordVO): { type: TagType, effect?: string, text: string } | null {
  if (row.status !== 'pending') return null
  if (!row.dueDate) return null
  const now = new Date()
  const due = parseLocalDate(row.dueDate)
  if (!due) return null
  now.setHours(0, 0, 0, 0)
  const daysUntilDue = Math.ceil((due.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  if (daysUntilDue < 0) return { type: 'danger', effect: 'dark', text: `逾期 ${Math.abs(daysUntilDue)} 天` }
  if (daysUntilDue <= 7) return { type: 'danger', effect: 'plain', text: '截止在即' }
  if (daysUntilDue <= 15) return { type: 'warning', effect: 'dark', text: '请尽快缴费' }
  if (daysUntilDue <= 30) return { type: 'primary', effect: 'dark', text: '即将缴费' }
  return null
}

function feeTagType(feeType: string): TagType {
  const map: Record<string, TagType> = {
    annual_fee: 'primary',
    registration_fee: 'success',
    maintenance_fee: 'warning',
    other: 'info',
  }
  return map[feeType] || 'info'
}

function statusTagType(status: string): TagType {
  const map: Record<string, TagType> = {
    pending: 'warning',
    paid: 'success',
    paused: 'info',
  }
  return map[status] || 'info'
}

function dueDateColor(row: FeeRecordVO): string {
  if (!row.dueDate || row.status === 'paid' || row.status === 'paused') return '#606266'
  const due = parseLocalDate(row.dueDate)
  if (!due) return '#606266'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const diff = Math.ceil((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
  if (diff < 0) return '#F56C6C' // overdue - red
  if (diff <= 7) return '#E6A23C' // 7 days - orange
  if (diff <= 15) return '#E6A23C' // 15 days - yellow/orange
  return '#606266' // normal
}

function fundSourceLabel(code: string | undefined): string {
  const map: Record<string, string> = {
    vertical: '纵向科研经费',
    horizontal: '横向科研经费',
    institute: '院配套',
    self: '自筹',
  }
  return code ? (map[code] || code) : '—'
}

// ── Init ──────────────────────────────────────────────────────────

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.fee-ledger {
  width: 100%;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #303133;
}

.quick-tags {
  margin-bottom: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.advanced-filters {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
