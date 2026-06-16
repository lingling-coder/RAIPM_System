<template>
  <div class="audit-log-page">
    <!-- Page Title -->
    <h2 class="page-title">审计日志</h2>

    <!-- Error Alert -->
    <el-alert
      v-if="errorState"
      title="审计日志加载失败"
      type="error"
      show-icon
      closable
      @close="errorState = false"
    >
      <template #default>
        <el-button type="text" @click="handleSearch">点击重试</el-button>
      </template>
    </el-alert>

    <!-- Search / Filter Panel -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline label-width="auto">
        <el-form-item label="操作人">
          <el-input
            v-model="searchForm.operatorName"
            placeholder="搜索操作人"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="searchForm.operationType"
            placeholder="全部类型"
            clearable
            style="width: 150px"
          >
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="新增" value="CREATE" />
            <el-option label="编辑" value="UPDATE" />
            <el-option label="删除" value="DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作对象">
          <el-input
            v-model="searchForm.targetType"
            placeholder="搜索操作对象"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :default-time="[new Date(0, 0, 0, 0, 0, 0), new Date(0, 0, 0, 23, 59, 59)]"
            style="width: 380px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Toolbar -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="default" icon="Download" @click="handleExport">导出</el-button>
        <el-button type="default" @click="handleChainVerify">完整性校验</el-button>
      </div>
    </div>

    <!-- Chain Verification Result -->
    <el-card v-if="chainResult" shadow="never" class="chain-result-card">
      <el-result
        v-if="chainResult.valid"
        icon="success"
        title="链条校验通过"
        :sub-title="`共 ${chainResult.totalChecked} 条日志，哈希链条完整`"
      />
      <div v-else>
        <el-result
          icon="error"
          title="链条校验发现断裂"
          :sub-title="`发现 ${chainResult.brokenLinks.length} 处链条断裂（共检查 ${chainResult.totalChecked} 条）`"
        />
        <el-table :data="chainResult.brokenLinks" border size="small" style="margin-top: 12px">
          <el-table-column prop="logId" label="日志ID" width="100" />
          <el-table-column prop="position" label="断裂位置" min-width="200" />
          <el-table-column prop="expectedHash" label="预期哈希" min-width="250">
            <template #default="{ row }">
              <code style="font-size: 12px">{{ row.expectedHash }}</code>
            </template>
          </el-table-column>
          <el-table-column prop="actualHash" label="实际哈希" min-width="250">
            <template #default="{ row }">
              <code style="font-size: 12px; color: #F56C6C">{{ row.actualHash }}</code>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Data Table -->
    <el-card shadow="never">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        border
        empty-text=" "
        style="width: 100%"
      >
        <template #empty>
          <el-empty
            v-if="!loading"
            description="暂无审计日志"
          >
            <template #description>
              <p>暂无审计日志</p>
              <p style="font-size: 12px; color: #909399; margin-top: 4px">调整筛选条件后重新查询</p>
            </template>
          </el-empty>
        </template>

        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="createdAt" label="操作时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="operationTypeTag(row.operationType)" size="small" effect="plain">
              {{ operationTypeLabel(row.operationType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operationName" label="操作对象" width="200" show-overflow-tooltip />
        <el-table-column prop="targetId" label="对象ID" width="120">
          <template #default="{ row }">
            <code style="font-size: 12px">{{ row.targetId || '-' }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP地址" width="130" />
        <el-table-column prop="status" label="操作结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button type="text" size="small" @click="openDetail(row as any)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- Detail Drawer (600px, read-only) -->
    <el-drawer
      v-model="drawerVisible"
      title="操作详情"
      size="600px"
      direction="rtl"
      :close-on-click-modal="true"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="操作时间">
            {{ formatDateTime(currentDetail.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ currentDetail.operatorName }}
          </el-descriptions-item>
          <el-descriptions-item label="操作类型">
            <el-tag :type="operationTypeTag(currentDetail.operationType)" size="small" effect="plain">
              {{ operationTypeLabel(currentDetail.operationType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作对象">
            {{ currentDetail.operationName }}
          </el-descriptions-item>
          <el-descriptions-item label="对象ID">
            <code>{{ currentDetail.targetId || '-' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="IP地址">
            {{ currentDetail.ipAddress }}
          </el-descriptions-item>
          <el-descriptions-item label="User-Agent">
            <span style="font-size: 12px; word-break: break-all">{{ currentDetail.userAgent || '-' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top: 20px; margin-bottom: 8px; font-size: 16px; font-weight: 600">变更内容</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="变更前内容">
            <el-input
              type="textarea"
              :model-value="currentDetail.originalContent || '（无）'"
              readonly
              :rows="6"
              style="width: 100%"
            />
          </el-descriptions-item>
          <el-descriptions-item label="变更后内容">
            <el-input
              type="textarea"
              :model-value="currentDetail.targetContent || '（无）'"
              readonly
              :rows="6"
              style="width: 100%"
            />
          </el-descriptions-item>
        </el-descriptions>

        <h3 style="margin-top: 20px; margin-bottom: 8px; font-size: 16px; font-weight: 600">哈希链</h3>
        <el-descriptions :column="1" border>
          <el-descriptions-item label="当前哈希">
            <code style="font-size: 12px; word-break: break-all">{{ currentDetail.currentHash }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="上一条哈希">
            <code style="font-size: 12px; word-break: break-all">{{ currentDetail.previousHash || '（首条记录）' }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="链条状态">
            <el-tag
              :type="currentDetail.integrityVerified ? 'success' : 'danger'"
              size="small"
            >
              {{ currentDetail.integrityVerified ? '链条完整' : '链条断裂' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <div v-else v-loading="detailLoading" class="drawer-loading" />
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { page as queryPage, getDetail, verifyChain } from '@/api/system/audit-log'
import type {
  AuditLogPageParams,
  AuditLogVO,
  ChainVerificationResult,
} from '@/api/system/audit-log'

// ── State ────────────────────────────────────────────────────────────────

const loading = ref(false)
const errorState = ref(false)
const tableData = ref<AuditLogVO[]>([])

const dateRange = ref<[string, string] | null>(null)

const searchForm = reactive({
  operatorName: '',
  operationType: '',
  targetType: '',
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// Detail drawer
const drawerVisible = ref(false)
const detailLoading = ref(false)
const currentDetail = ref<AuditLogVO | null>(null)

// Chain verification result
const chainResult = ref<ChainVerificationResult | null>(null)

// ── Hooks ─────────────────────────────────────────────────────────────────

onMounted(() => {
  // Default time range: last 7 days
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  dateRange.value = [
    formatDateString(sevenDaysAgo),
    formatDateString(now),
  ]
  fetchData()
})

// ── Data Fetching ─────────────────────────────────────────────────────────

async function fetchData() {
  loading.value = true
  errorState.value = false
  chainResult.value = null

  try {
    const params: AuditLogPageParams = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      operatorName: searchForm.operatorName || undefined,
      operationType: searchForm.operationType || undefined,
      targetType: searchForm.targetType || undefined,
    }

    if (dateRange.value && dateRange.value[0] && dateRange.value[1]) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    const response = await queryPage(params)
    if (response.data && response.data.code === 200) {
      const pageResult = response.data.data
      tableData.value = pageResult.records || []
      pagination.total = pageResult.total || 0
    } else {
      errorState.value = true
    }
  } catch (e) {
    console.error('Failed to fetch audit logs:', e)
    errorState.value = true
  } finally {
    loading.value = false
  }
}

// ── Search / Reset ────────────────────────────────────────────────────────

function handleSearch() {
  pagination.page = 1
  chainResult.value = null
  fetchData()
}

function handleReset() {
  searchForm.operatorName = ''
  searchForm.operationType = ''
  searchForm.targetType = ''
  // Reset date range to last 7 days
  const now = new Date()
  const sevenDaysAgo = new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000)
  dateRange.value = [
    formatDateString(sevenDaysAgo),
    formatDateString(now),
  ]
  pagination.page = 1
  chainResult.value = null
  fetchData()
}

// ── Detail Drawer ─────────────────────────────────────────────────────────

async function openDetail(row: AuditLogVO) {
  drawerVisible.value = true
  detailLoading.value = true
  currentDetail.value = null

  try {
    const response = await getDetail(row.id)
    if (response.data && response.data.code === 200) {
      currentDetail.value = response.data.data
    }
  } catch (e) {
    console.error('Failed to fetch audit log detail:', e)
  } finally {
    detailLoading.value = false
  }
}

// ── Chain Verification ────────────────────────────────────────────────────

async function handleChainVerify() {
  if (tableData.value.length === 0) {
    ElMessage.info('暂无数据可校验')
    return
  }

  const ids = tableData.value.map((item) => item.id).sort((a, b) => a - b)
  const fromId = ids[0]
  const toId = ids[ids.length - 1]

  try {
    const response = await verifyChain(fromId, toId)
    if (response.data && response.data.code === 200) {
      chainResult.value = response.data.data
    } else {
      ElMessage.error('校验失败')
    }
  } catch (e) {
    console.error('Chain verification failed:', e)
    ElMessage.error('校验请求失败')
  }
}

// ── Export CSV ────────────────────────────────────────────────────────────

function handleExport() {
  // Build CSV from current table data
  if (tableData.value.length === 0) {
    ElMessage.info('暂无数据可导出')
    return
  }

  const headers = [
    '操作时间', '操作人', '操作类型', '操作对象',
    '对象ID', 'IP地址', '操作结果', '当前哈希',
  ]
  const rows = tableData.value.map((row) => [
    formatDateTime(row.createdAt),
    row.operatorName,
    operationTypeLabel(row.operationType),
    row.operationName,
    row.targetId || '',
    row.ipAddress,
    row.status === 1 ? '成功' : '失败',
    row.currentHash,
  ])

  const csvContent = [
    headers.join(','),
    ...rows.map((r) => r.map((cell) => `"${cell}"`).join(',')),
  ].join('\n')

  const blob = new Blob(['﻿' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `审计日志_${formatDateString(new Date())}.csv`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// ── Helpers ───────────────────────────────────────────────────────────────

function operationTypeLabel(type: string): string {
  const map: Record<string, string> = {
    LOGIN: '登录',
    LOGOUT: '登出',
    CREATE: '新增',
    UPDATE: '编辑',
    DELETE: '删除',
  }
  return map[type] || type
}

function operationTypeTag(type: string): 'primary' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, 'primary' | 'success' | 'warning' | 'info' | 'danger'> = {
    LOGIN: 'info',
    LOGOUT: 'info',
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'danger',
  }
  return map[type] || 'info'
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  try {
    const d = new Date(dateStr)
    const pad = (n: number) => n.toString().padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  } catch {
    return dateStr
  }
}

function formatDateString(date: Date): string {
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
</script>

<script lang="ts">
export default {
  name: 'SystemAuditLog',
}
</script>

<style scoped>
.audit-log-page {
  padding: 16px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.search-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  gap: 8px;
}

.chain-result-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.drawer-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}
</style>
