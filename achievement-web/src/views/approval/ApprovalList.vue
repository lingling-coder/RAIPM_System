<template>
  <div class="approval-list" v-loading="loading">
    <h2 class="page-title">审批待办</h2>

    <!-- Filter Bar -->
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="成果类型">
        <el-select v-model="filters.type" placeholder="全部" clearable style="width: 140px">
          <el-option label="论文" value="paper" />
          <el-option label="专利" value="patent" />
          <el-option label="软件著作权" value="copyright" />
        </el-select>
      </el-form-item>
      <el-form-item label="提交时间">
        <el-date-picker
          v-model="filters.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
        />
      </el-form-item>
      <el-form-item>
        <el-input
          v-model="filters.keyword"
          placeholder="关键词搜索"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- Approval Table -->
    <el-table :data="tableData" stripe @row-click="handleRowClick" style="cursor: pointer">
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="类型" width="120">
        <template #default="scope">
          <el-tag :type="typeTag(scope.row.type)" size="small">{{ typeLabel(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitterName" label="提交人" width="120" />
      <el-table-column prop="deptName" label="所属部门" width="120" />
      <el-table-column prop="submitTime" label="提交时间" width="160" />
    </el-table>

    <!-- Empty State -->
    <el-empty v-if="!loading && tableData.length === 0" description="暂无待审批项">
      <el-icon :size="40" color="#909399"><CircleCheck /></el-icon>
    </el-empty>

    <!-- Pagination -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="fetchData"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck } from '@element-plus/icons-vue'
import * as approvalApi from '@/api/approval'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

const filters = reactive({
  type: null as string | null,
  dateRange: null as [string, string] | null,
  keyword: '',
})

onMounted(() => {
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await approvalApi.getPending({
      page: currentPage.value,
      size: pageSize.value,
      type: filters.type || undefined,
      startDate: filters.dateRange ? filters.dateRange[0] : undefined,
      endDate: filters.dateRange ? filters.dateRange[1] : undefined,
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

function handleSearch() {
  currentPage.value = 1
  fetchData()
}

function handleReset() {
  filters.type = null
  filters.dateRange = null
  filters.keyword = ''
  currentPage.value = 1
  fetchData()
}

function handleRowClick(row: any) {
  router.push(`/approval/detail/${row.id}?type=${row.type || row.achievementType}`)
}

function typeTag(type: string): string {
  const map: Record<string, string> = { paper: '', patent: 'success', copyright: 'warning' }
  return map[type] || 'info'
}

function typeLabel(type: string): string {
  const map: Record<string, string> = { paper: '论文', patent: '专利', copyright: '软件著作权' }
  return map[type] || type
}
</script>

<style scoped>
.approval-list {
  width: 100%;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 20px 0;
}

.filter-bar {
  margin-bottom: 16px;
}
</style>
