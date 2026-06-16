<template>
  <div class="fee-plan">
    <div class="page-header">
      <h2 class="page-title">缴费计划</h2>
      <el-button type="primary" @click="openCreate">新建缴费计划</el-button>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <el-select v-model="filters.status" placeholder="计划状态" clearable style="width: 130px" @change="fetchData">
        <el-option label="启用中" value="active" />
        <el-option label="已暂停" value="paused" />
      </el-select>

      <el-select v-model="filters.feeType" placeholder="费用类型" clearable style="width: 140px" @change="fetchData">
        <el-option label="专利年费" value="annual_fee" />
        <el-option label="登记费" value="registration_fee" />
        <el-option label="维护费" value="maintenance_fee" />
        <el-option label="其他" value="other" />
      </el-select>

      <el-input
        v-model="filters.keyword"
        placeholder="搜索专利名称"
        clearable
        style="width: 180px"
        @keyup.enter="fetchData"
      />

      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- Table -->
    <el-table v-loading="loading" :data="tableData" style="width: 100%; margin-top: 16px">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column label="关联专利" min-width="180" show-overflow-tooltip>
        <template #default="scope">
          <el-link type="primary" :underline="false" @click.stop="goToPatent(asPlan(scope.row))">
            {{ asPlan(scope.row).patentName || '—' }}
          </el-link>
        </template>
      </el-table-column>
      <el-table-column label="费用类型" width="120">
        <template #default="scope">
          <el-tag :type="feeTagType(scope.row.feeType)" size="small">
            {{ scope.row.feeTypeLabel || scope.row.feeType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="截止日期" width="120">
        <template #default="scope">
          {{ scope.row.dueDate || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="金额" width="120" align="right">
        <template #default="scope">
          ¥{{ scope.row.amount ? scope.row.amount.toFixed(2) : '0.00' }}
        </template>
      </el-table-column>
      <el-table-column label="经费来源" width="120">
        <template #default="scope">
          {{ fundSourceLabel(scope.row.fundingSource) }}
        </template>
      </el-table-column>
      <el-table-column label="来源" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.source === 'auto_generated' ? 'primary' : 'info'" size="small">
            {{ scope.row.source === 'auto_generated' ? '自动生成' : '手动创建' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'active' ? 'success' : 'warning'" size="small">
            {{ scope.row.statusLabel || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" link @click.stop="openEdit(asPlan(scope.row))">编辑</el-button>
          <el-button
            v-if="scope.row.status === 'active'"
            size="small"
            type="warning"
            link
            @click.stop="handlePause(asPlan(scope.row))"
          >
            暂停
          </el-button>
          <el-button
            v-if="scope.row.status === 'paused'"
            size="small"
            type="success"
            link
            @click.stop="handleRestore(asPlan(scope.row))"
          >
            恢复
          </el-button>
          <el-button
            v-if="scope.row.status === 'paused'"
            size="small"
            type="danger"
            link
            @click.stop="handleDelete(asPlan(scope.row))"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Empty state -->
    <el-empty v-if="!loading && tableData.length === 0" description="暂无缴费计划" />

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

    <!-- Edit dialog (D-17: dueDate locked, amount editable) -->
    <el-dialog v-model="editDialogVisible" title="编辑缴费计划" width="500px">
      <el-form v-if="editForm" :model="editForm" label-width="110px">
        <el-form-item label="关联专利">
          <el-input :model-value="editForm.patentName" disabled />
        </el-form-item>
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
        <el-form-item label="经费来源">
          <FundSourceSelect v-model="editForm.fundingSource" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- Create dialog (新建缴费计划) -->
    <el-dialog v-model="createDialogVisible" title="新建缴费计划" width="550px">
      <el-form :model="createForm" label-width="110px" :rules="createRules" ref="createFormRef">
        <el-form-item label="关联专利" prop="patentId">
          <el-select
            v-model="createForm.patentId"
            placeholder="搜索并选择专利"
            filterable
            remote
            :remote-method="searchPatent"
            :loading="patentLoading"
            style="width: 100%"
          >
            <el-option
              v-for="patent in patentOptions"
              :key="patent.id"
              :label="`${patent.patentName} (${patent.applicationNo || '—'})`"
              :value="patent.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="费用类型" prop="feeType">
          <el-select v-model="createForm.feeType" placeholder="选择费用类型" style="width: 100%">
            <el-option label="专利年费" value="annual_fee" />
            <el-option label="登记费" value="registration_fee" />
            <el-option label="维护费" value="maintenance_fee" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期" prop="dueDate">
          <el-date-picker v-model="createForm.dueDate" type="date" placeholder="选择截止日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="createForm.amount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="经费来源">
          <FundSourceSelect v-model="createForm.fundingSource" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="saveCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as feePlanApi from '@/api/fee/feePlan'
import type { FeePlanVO } from '@/api/fee/feePlan'
import FundSourceSelect from '@/components/fee/FundSourceSelect.vue'
import http from '@/api/index'

/** TagType used by Element Plus el-tag */
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const router = useRouter()

// ── State ──────────────────────────────────────────────────────────
const loading = ref(false)
const editLoading = ref(false)
const createLoading = ref(false)
const patentLoading = ref(false)
const tableData = ref<FeePlanVO[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const editDialogVisible = ref(false)
const createDialogVisible = ref(false)
const editForm = ref<any>(null)
const currentEditId = ref<number | null>(null)
const createFormRef = ref<any>(null)
const patentOptions = ref<any[]>([])

const createForm = reactive({
  patentId: null as number | null,
  feeType: null as string | null,
  amount: 0,
  dueDate: null as any,
  fundingSource: null as string | null,
})

const createRules = {
  patentId: [{ required: true, message: '请选择关联专利', trigger: 'change' }],
  feeType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  dueDate: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
}

const filters = reactive({
  status: null as string | null,
  feeType: null as string | null,
  keyword: '',
})

// ── Fetch data ────────────────────────────────────────────────────

async function fetchData() {
  loading.value = true

  try {
    const res: any = await feePlanApi.getPage({
      page: page.value,
      size: pageSize.value,
      status: filters.status || undefined,
      feeType: filters.feeType || undefined,
      keyword: filters.keyword || undefined,
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
  filters.status = null
  filters.feeType = null
  filters.keyword = ''
  page.value = 1
  fetchData()
}

// ── Helpers ───────────────────────────────────────────────────────
/** Cast table row to FeePlanVO for type safety in el-table scoped slots */
function asPlan(row: any): FeePlanVO { return row as FeePlanVO }

// ── Navigation ────────────────────────────────────────────────────

function goToPatent(row: FeePlanVO) {
  if (row.patentId) {
    router.push(`/achievement/detail/${row.patentId}`)
  }
}

// ── Pause / Restore / Delete ──────────────────────────────────────

async function handlePause(row: FeePlanVO) {
  try {
    await ElMessageBox.confirm(
      '确认暂停该缴费计划？暂停期间不会产生预警。',
      '确认暂停',
      { type: 'warning' }
    )
    await feePlanApi.pausePlan(row.id)
    ElMessage.success('缴费计划已暂停')
    fetchData()
  } catch {
    // cancelled
  }
}

async function handleRestore(row: FeePlanVO) {
  try {
    await ElMessageBox.confirm(
      '确认恢复该缴费计划？恢复后将重新开始预警。',
      '确认恢复',
      { type: 'info' }
    )
    await feePlanApi.restorePlan(row.id)
    ElMessage.success('缴费计划已恢复')
    fetchData()
  } catch {
    // cancelled
  }
}

async function handleDelete(row: FeePlanVO) {
  if (row.status !== 'paused') {
    ElMessage.warning('仅已暂停的缴费计划可以删除')
    return
  }

  try {
    await ElMessageBox.confirm(
      '确认删除该缴费计划？此操作不可撤销。',
      '确认删除',
      { type: 'warning' }
    )
    await feePlanApi.remove(row.id)
    ElMessage.success('缴费计划已删除')
    fetchData()
  } catch {
    // cancelled
  }
}

// ── Edit dialog ───────────────────────────────────────────────────

function openEdit(row: FeePlanVO) {
  currentEditId.value = row.id
  editForm.value = {
    patentName: row.patentName,
    feeType: row.feeType,
    feeTypeLabel: row.feeTypeLabel,
    dueDate: row.dueDate ? new Date(row.dueDate) : null,
    amount: row.amount,
    fundingSource: row.fundingSource,
  }
  editDialogVisible.value = true
}

async function saveEdit() {
  if (!currentEditId.value || !editForm.value) return

  editLoading.value = true
  try {
    await feePlanApi.update(currentEditId.value, {
      patentId: 0, // Not used in update, but required by DTO
      feeType: editForm.value.feeType,
      amount: editForm.value.amount,
      dueDate: editForm.value.dueDate ? editForm.value.dueDate.toISOString().slice(0, 10) : undefined,
      fundingSource: editForm.value.fundingSource || undefined,
    })
    ElMessage.success('缴费计划已更新')
    editDialogVisible.value = false
    fetchData()
  } catch {
    // error handled by API interceptor
  } finally {
    editLoading.value = false
  }
}

// ── Create dialog ─────────────────────────────────────────────────

async function searchPatent(query: string) {
  if (!query) {
    patentOptions.value = []
    return
  }
  patentLoading.value = true
  try {
    const res: any = await http.get('/api/patents/list', {
      params: { keyword: query, page: 1, size: 20 },
    })
    if (res?.data?.records) {
      patentOptions.value = res.data.records
    } else if (Array.isArray(res?.data)) {
      patentOptions.value = res.data
    } else {
      patentOptions.value = []
    }
  } catch {
    patentOptions.value = []
  } finally {
    patentLoading.value = false
  }
}

function openCreate() {
  createForm.patentId = null
  createForm.feeType = null
  createForm.amount = 0
  createForm.dueDate = null
  createForm.fundingSource = null
  patentOptions.value = []
  createDialogVisible.value = true
}

async function saveCreate() {
  if (!createFormRef.value) return

  const valid = await createFormRef.value.validate().catch(() => false)
  if (!valid) return

  createLoading.value = true
  try {
    await feePlanApi.create({
      patentId: createForm.patentId!,
      feeType: createForm.feeType!,
      amount: createForm.amount,
      dueDate: createForm.dueDate ? createForm.dueDate.toISOString().slice(0, 10) : undefined,
      fundingSource: createForm.fundingSource || undefined,
    })
    ElMessage.success('缴费计划创建成功')
    createDialogVisible.value = false
    page.value = 1
    fetchData()
  } catch {
    // error handled by API interceptor
  } finally {
    createLoading.value = false
  }
}

// ── Display helpers ───────────────────────────────────────────────

function feeTagType(feeType: string): TagType {
  const map: Record<string, TagType> = {
    annual_fee: 'primary',
    registration_fee: 'success',
    maintenance_fee: 'warning',
    other: 'info',
  }
  return map[feeType] || 'info'
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
.fee-plan {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
