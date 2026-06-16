<template>
  <div class="achievement-list">
    <h2 class="page-title">成果列表</h2>

    <!-- Tabs: Active / Draft / Invalidated -->
    <el-tabs v-model="activeTab" @tab-change="fetchData">
      <el-tab-pane label="活跃" name="active" />
      <el-tab-pane label="草稿" name="draft" />
      <el-tab-pane label="已作废" name="invalidated" />
    </el-tabs>

    <!-- Filter bar -->
    <div class="filter-bar">
      <el-select v-model="filters.type" placeholder="成果类型" clearable style="width: 150px" @change="fetchData">
        <el-option label="论文" value="paper" />
        <el-option label="专利" value="patent" />
        <el-option label="软件著作权" value="copyright" />
      </el-select>

      <el-date-picker
        v-model="filters.dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        style="width: 240px"
      />

      <el-input
        v-model="filters.keyword"
        placeholder="搜索标题/名称"
        clearable
        style="width: 200px"
        @keyup.enter="fetchData"
      />

      <el-button type="primary" @click="fetchData">搜索</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <!-- Table -->
    <el-table
      v-loading="loading"
      :data="tableData"
      style="width: 100%"
      @row-click="goToDetail"
    >
      <el-table-column type="index" label="#" width="60" />
      <el-table-column label="标题/名称" min-width="200" show-overflow-tooltip>
        <template #default="scope">
          {{ scope.row.title || scope.row.patentName || scope.row.name }}
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120">
        <template #default="scope">
          <el-tag>{{ typeLabel(scope.row) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="密级" width="80" align="center">
        <template #default="scope">
          <ClassifiedTag :level="scope.row.classifiedLevel" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="140">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)">
            {{ scope.row.statusLabel || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交人" width="120">
        <template #default="scope">
          {{ scope.row.submitterName || scope.row.createdBy || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="提交时间" width="180" />
    </el-table>

    <!-- Empty state -->
    <el-empty v-if="!loading && tableData.length === 0" :description="emptyText">
      <el-button type="primary" @click="$router.push('/achievement/register')">登记成果</el-button>
    </el-empty>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import * as paperApi from '@/api/achievement/paper'
import * as patentApi from '@/api/achievement/patent'
import * as copyrightApi from '@/api/achievement/copyright'
import ClassifiedTag from '@/components/achievement/ClassifiedTag.vue'

const router = useRouter()

// ── State ──────────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const activeTab = ref('active')
const currentApiType = ref<'paper' | 'patent' | 'copyright' | null>(null)

const filters = reactive({
  type: null as string | null,
  dateRange: null as any,
  keyword: '',
})

// ── Type label mapping ────────────────────────────────────────────

function typeLabel(row: any): string {
  // Determine type from row data shape or explicit field
  if (row.title !== undefined) return '论文'
  if (row.patentName !== undefined) return '专利'
  if (row.name !== undefined) return '软件著作权'
  // Fall back to tracking which API was used
  if (currentApiType.value === 'paper') return '论文'
  if (currentApiType.value === 'patent') return '专利'
  if (currentApiType.value === 'copyright') return '软件著作权'
  return '未知'
}

// ── Status tag type mapping ───────────────────────────────────────

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING_DEPT_REVIEW: 'warning',
    PENDING_ADMIN_ARCHIVE: 'primary',
    ARCHIVED: 'success',
    REJECTED: 'danger',
    INVALIDATED: 'info',
    WITHDRAWN: 'info',
  }
  return map[status] || 'info'
}

// ── Empty state text ──────────────────────────────────────────────

const emptyTextMap: Record<string, string> = {
  active: '暂无活跃成果，点击右上角"登记成果"开始登记',
  draft: '暂无草稿',
  invalidated: '暂无已作废成果',
}

const emptyText = ref(emptyTextMap.active)

// ── Fetch data ────────────────────────────────────────────────────

async function fetchData() {
  loading.value = true
  emptyText.value = emptyTextMap[activeTab.value] || emptyTextMap.active

  try {
    const statusMap: Record<string, string | undefined> = {
      active: undefined,
      draft: 'DRAFT',
      invalidated: 'INVALIDATED',
    }

    // Determine which API to call based on type filter
    const type = filters.type
    const api = type === 'patent' ? patentApi : type === 'copyright' ? copyrightApi : paperApi
    currentApiType.value = (type as 'paper' | 'patent' | 'copyright') || 'paper'

    const res: any = await api.getPage({
      page: page.value,
      size: pageSize.value,
      status: statusMap[activeTab.value],
      keyword: filters.keyword || undefined,
      dateRange: filters.dateRange || undefined,
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

// ── Filters ───────────────────────────────────────────────────────

function resetFilters() {
  filters.type = null
  filters.dateRange = null
  filters.keyword = ''
  page.value = 1
  fetchData()
}

// ── Navigation ────────────────────────────────────────────────────

function goToDetail(row: any) {
  router.push(`/achievement/detail/${row.id}`)
}

// ── Init ──────────────────────────────────────────────────────────

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.achievement-list {
  width: 100%;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #303133;
}

.filter-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
