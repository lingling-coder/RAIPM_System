<template>
  <div class="fee-stats">
    <h2 class="page-title">费用统计</h2>

    <!-- ── Overview Cards (D-27) ────────────────────────────────── -->
    <el-row :gutter="20" class="overview-cards">
      <el-col :span="6">
        <el-card shadow="never" class="stats-card card-total">
          <div class="card-body">
            <div class="card-icon icon-total">
              <el-icon :size="28"><Coin /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">费用总额</div>
              <div class="card-value">{{ formatAmount(overview.totalAmount) }}</div>
              <div class="card-footer">{{ overview.recordCount }} 条记录</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stats-card card-paid">
          <div class="card-body">
            <div class="card-icon icon-paid">
              <el-icon :size="28"><SuccessFilled /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">已缴费</div>
              <div class="card-value">{{ formatAmount(overview.totalPaid) }}</div>
              <div class="card-footer">占 {{ paymentRate(overview.totalPaid, overview.totalAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stats-card card-pending">
          <div class="card-body">
            <div class="card-icon icon-pending">
              <el-icon :size="28"><WarningFilled /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">待缴费</div>
              <div class="card-value">{{ formatAmount(overview.totalPending) }}</div>
              <div class="card-footer">占 {{ paymentRate(overview.totalPending, overview.totalAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never" class="stats-card card-overdue">
          <div class="card-body">
            <div class="card-icon icon-overdue">
              <el-icon :size="28"><CircleCloseFilled /></el-icon>
            </div>
            <div class="card-info">
              <div class="card-label">逾期金额</div>
              <div class="card-value">{{ formatAmount(overview.totalOverdue) }}</div>
              <div class="card-footer">占 {{ paymentRate(overview.totalOverdue, overview.totalAmount) }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Filter Row ──────────────────────────────────────────── -->
    <div class="filter-bar">
      <span class="filter-label">统计维度：</span>
      <el-select v-model="currentDimension" style="width: 160px" @change="onDimensionChange">
        <el-option label="部门" value="dept_id" />
        <el-option label="年份" value="YEAR(due_date)" />
        <el-option label="专利类型" value="patent_type" />
        <el-option label="经费来源" value="funding_source" />
      </el-select>

      <el-divider direction="vertical" />

      <span class="filter-label">筛选：</span>

      <el-select v-model="filters.deptId" placeholder="部门" clearable style="width: 140px">
        <el-option
          v-for="d in departmentOptions"
          :key="d.id"
          :label="d.deptName"
          :value="d.id"
        />
      </el-select>

      <el-select v-model="filters.year" placeholder="年份" clearable style="width: 120px">
        <el-option
          v-for="y in yearOptions"
          :key="y"
          :label="String(y)"
          :value="y"
        />
      </el-select>

      <el-select v-model="filters.patentType" placeholder="专利类型" clearable style="width: 150px">
        <el-option label="发明专利" value="发明" />
        <el-option label="实用新型" value="实用新型" />
        <el-option label="外观设计" value="外观设计" />
      </el-select>

      <el-select v-model="filters.fundingSource" placeholder="经费来源" clearable style="width: 150px">
        <el-option label="纵向科研经费" value="vertical" />
        <el-option label="横向科研经费" value="horizontal" />
        <el-option label="院配套" value="institute" />
        <el-option label="自筹" value="self" />
      </el-select>

      <el-button type="primary" :loading="loading" @click="onQuery">
        <el-icon><Search /></el-icon>
        查询
      </el-button>

      <el-button :loading="exportLoading" @click="handleExport">
        <el-icon><Download /></el-icon>
        导出Excel
      </el-button>
    </div>

    <!-- ── Statistics Table ────────────────────────────────────── -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%; margin-top: 16px"
      border
      :summary-method="summaryMethod"
      show-summary
    >
      <!-- Dynamic dimension column -->
      <el-table-column :label="dimensionLabel" min-width="160">
        <template #default="scope">
          <span class="dimension-cell">{{ getDimensionDisplay(scope.row) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="记录数" prop="recordCount" width="100" align="right" sortable />

      <el-table-column label="总金额" width="140" align="right" sortable>
        <template #default="scope">
          {{ formatAmount(scope.row.totalAmount) }}
        </template>
      </el-table-column>

      <el-table-column label="已缴费" width="140" align="right" sortable>
        <template #default="scope">
          <span class="amount-paid">{{ formatAmount(scope.row.totalPaid) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="待缴费" width="140" align="right" sortable>
        <template #default="scope">
          <span class="amount-pending">{{ formatAmount(scope.row.totalPending) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="逾期金额" width="140" align="right" sortable>
        <template #default="scope">
          <span class="amount-overdue">{{ formatAmount(scope.row.totalOverdue) }}</span>
        </template>
      </el-table-column>

      <el-table-column label="缴费率" width="110" align="center">
        <template #default="scope">
          <el-tag :type="rateTagType(scope.row.totalPaid, scope.row.totalAmount)" size="small">
            {{ paymentRate(scope.row.totalPaid, scope.row.totalAmount) }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <!-- Empty state -->
    <el-empty v-if="!loading && tableData.length === 0" description="暂无统计数据" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Coin,
  SuccessFilled,
  WarningFilled,
  CircleCloseFilled,
  Search,
  Download,
} from '@element-plus/icons-vue'
import * as feeStatsApi from '@/api/fee/feeStats'
import type { FeeStatsVO } from '@/api/fee/feeStats'

// ── Constants ──────────────────────────────────────────────────────────

/** Patent type code to Chinese label mapping */
const PATENT_TYPE_LABELS: Record<string, string> = {
  invention: '发明专利',
  utility_model: '实用新型',
  design: '外观设计',
  发明: '发明专利',
  实用新型: '实用新型',
  外观设计: '外观设计',
  other: '其他',
}

/** Funding source code to Chinese label mapping */
const FUND_SOURCE_LABELS: Record<string, string> = {
  vertical: '纵向科研经费',
  horizontal: '横向科研经费',
  institute: '院配套',
  self: '自筹',
}

/** Dimension value to Chinese column header mapping */
const DIMENSION_LABELS: Record<string, string> = {
  dept_id: '部门',
  'YEAR(due_date)': '年份',
  patent_type: '专利类型',
  funding_source: '经费来源',
}

// ── State ──────────────────────────────────────────────────────────────

const loading = ref(false)
const exportLoading = ref(false)
const overview = reactive<FeeStatsVO>({
  totalAmount: 0,
  totalPaid: 0,
  totalPending: 0,
  totalOverdue: 0,
  recordCount: 0,
})
const tableData = ref<FeeStatsVO[]>([])
const currentDimension = ref('dept_id')

const filters = reactive({
  deptId: null as number | null,
  year: null as number | null,
  patentType: null as string | null,
  fundingSource: null as string | null,
})

// ── Options ────────────────────────────────────────────────────────────

interface DeptOption {
  id: number
  deptName: string
}

const departmentOptions = ref<DeptOption[]>([])
const currentYear = new Date().getFullYear()
const yearOptions = computed(() => {
  const years: number[] = []
  for (let y = currentYear - 5; y <= currentYear + 2; y++) {
    years.push(y)
  }
  return years
})

// ── Computed ───────────────────────────────────────────────────────────

const dimensionLabel = computed(() => {
  return DIMENSION_LABELS[currentDimension.value] || '维度'
})

// ── Data Fetching ──────────────────────────────────────────────────────

async function fetchOverview() {
  try {
    const params = buildParams()
    const res: any = await feeStatsApi.getOverview(params)
    if (res?.data) {
      Object.assign(overview, res.data)
    }
  } catch {
    // Error handled by API interceptor
  }
}

async function fetchDimensionStats() {
  try {
    const params = buildParams()
    const res: any = await feeStatsApi.getDimensionStats(currentDimension.value, params)
    if (res?.data) {
      tableData.value = res.data || []
    }
  } catch {
    tableData.value = []
  }
}

async function onQuery() {
  loading.value = true
  try {
    await Promise.all([fetchOverview(), fetchDimensionStats()])
  } finally {
    loading.value = false
  }
}

function onDimensionChange() {
  fetchDimensionStats()
}

function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (filters.deptId != null) params.deptId = filters.deptId
  if (filters.year != null) params.year = filters.year
  if (filters.patentType) params.patentType = filters.patentType
  if (filters.fundingSource) params.fundingSource = filters.fundingSource
  return params
}

// ── Excel Export ───────────────────────────────────────────────────────

async function handleExport() {
  exportLoading.value = true
  try {
    const params = buildParams()
    const res: any = await feeStatsApi.exportExcel(params)

    let blob: Blob
    if (res instanceof Blob) {
      blob = res
    } else if (res?.data instanceof Blob) {
      blob = res.data
    } else {
      ElMessage.error('导出失败：无法获取文件数据')
      return
    }

    const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    const filename = `fee-stats-${currentDimension.value}-${dateStr}.xlsx`
    downloadBlob(blob, filename)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

function downloadBlob(blob: Blob, filename: string) {
  const url = window.URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(url)
}

// ── Table Summary Row ──────────────────────────────────────────────────

function summaryMethod(param: { columns: any[]; data: FeeStatsVO[] }) {
  const { columns, data } = param
  const sums: (string | number)[] = []

  columns.forEach((column: any, index: number) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }

    const prop = column.property
    if (!prop) {
      sums[index] = ''
      return
    }

    const values = data.map((item) => Number(item[prop as keyof FeeStatsVO] || 0))
    if (values.length === 0) {
      sums[index] = ''
      return
    }

    if (prop === 'recordCount') {
      sums[index] = values.reduce((a, b) => a + b, 0)
    } else if (['totalAmount', 'totalPaid', 'totalPending', 'totalOverdue'].includes(prop)) {
      const total = values.reduce((a, b) => a + b, 0)
      sums[index] = '¥' + total.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    }
  })

  return sums
}

// ── Display Helpers ────────────────────────────────────────────────────

function formatAmount(value: number | undefined | null): string {
  if (value == null || isNaN(value)) return '¥0.00'
  return '¥' + value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function paymentRate(paid: number | undefined | null, total: number | undefined | null): string {
  if (!total || total === 0) return '0.00%'
  if (!paid || paid === 0) return '0.00%'
  const rate = (paid / total) * 100
  return rate.toFixed(2) + '%'
}

function rateTagType(paid: number | undefined | null, total: number | undefined | null): string {
  if (!total || total === 0) return 'info'
  if (!paid || paid === 0) return 'danger'
  const rate = paid / total
  if (rate >= 0.8) return 'success'
  if (rate >= 0.5) return 'warning'
  return 'danger'
}

function getDimensionDisplay(row: FeeStatsVO): string {
  const val = row.dimensionValue || row.dimensionCode || '—'
  const dim = currentDimension.value

  if (dim === 'patent_type') {
    return PATENT_TYPE_LABELS[val] || val
  }
  if (dim === 'funding_source') {
    return FUND_SOURCE_LABELS[val] || val
  }
  return val
}

// ── Initialisation ─────────────────────────────────────────────────────

onMounted(async () => {
  loading.value = true
  try {
    await Promise.all([fetchOverview(), fetchDimensionStats()])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.fee-stats {
  width: 100%;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 24px;
  color: #303133;
}

/* ── Overview Cards ──────────────────────────────────────────── */

.overview-cards {
  margin-bottom: 24px;
}

.stats-card {
  border-radius: 8px;
  transition: box-shadow 0.3s;
}

.stats-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.card-body {
  display: flex;
  align-items: center;
  gap: 16px;
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.icon-total {
  background: #ecf5ff;
  color: #409eff;
}

.icon-paid {
  background: #f0f9eb;
  color: #67c23a;
}

.icon-pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.icon-overdue {
  background: #fef0f0;
  color: #f56c6c;
}

.card-info {
  flex: 1;
  min-width: 0;
}

.card-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}

.card-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-footer {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 2px;
}

/* ── Filter Bar ──────────────────────────────────────────────── */

.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

/* ── Table ───────────────────────────────────────────────────── */

.dimension-cell {
  font-weight: 500;
  color: #303133;
}

.amount-paid {
  color: #67c23a;
}

.amount-pending {
  color: #e6a23c;
}

.amount-overdue {
  color: #f56c6c;
}

/* ── Empty state styling ─────────────────────────────────────── */
.el-empty {
  margin-top: 40px;
}
</style>
