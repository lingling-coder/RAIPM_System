<template>
  <div class="search-results-page">
    <!-- Search info bar: keyword + result count -->
    <div class="search-info">
      <h2 class="search-info-title">搜索结果: {{ routeQueryKeyword }}</h2>
      <span v-if="!loading" class="search-info-count">共 {{ total }} 条结果</span>
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <span class="filter-label">筛选：</span>
      <el-select
        v-model="filters.type"
        placeholder="成果类型"
        clearable
        @change="fetchResults"
        style="width: 130px;"
      >
        <el-option label="全部" value="" />
        <el-option label="论文" value="paper" />
        <el-option label="专利" value="patent" />
        <el-option label="软著" value="software" />
      </el-select>
      <el-select
        v-model="filters.deptId"
        placeholder="所属部门"
        clearable
        @change="fetchResults"
        style="width: 150px;"
      >
        <el-option label="全部部门" :value="null" />
        <el-option
          v-for="d in departments"
          :key="d.id"
          :label="d.deptName"
          :value="d.id"
        />
      </el-select>
      <el-date-picker
        v-model="yearRange"
        type="yearrange"
        value-format="YYYY"
        start-placeholder="开始年份"
        end-placeholder="结束年份"
        range-separator="至"
        @change="handleYearChange"
        style="width: 260px;"
      />
      <!-- D-16: Classification filter only shown for classified managers -->
      <el-select
        v-if="isClassifiedManager"
        v-model="filters.classification"
        placeholder="密级"
        clearable
        @change="fetchResults"
        style="width: 120px;"
      >
        <el-option label="全部" value="" />
        <el-option label="普通" value="NORMAL" />
        <el-option label="涉密" value="CLASSIFIED" />
      </el-select>
    </div>

    <!-- Loading state -->
    <template v-if="loading">
      <el-skeleton :rows="5" animated />
    </template>

    <!-- Error state -->
    <template v-else-if="error">
      <el-alert title="搜索失败，请稍后重试" type="error" show-icon>
        <template #default>
          <el-button type="text" @click="fetchResults">点击重试</el-button>
        </template>
      </el-alert>
    </template>

    <!-- Empty state -->
    <template v-else-if="total === 0">
      <el-empty
        :description="`未找到与&quot;${routeQueryKeyword}&quot;相关的成果，请尝试其他关键词或调整筛选条件`"
      />
    </template>

    <!-- Results list -->
    <template v-else>
      <div class="results-list">
        <div
          v-for="item in results"
          :key="item.achievementType + '-' + item.id"
          class="result-item"
          @click="goToDetail(item)"
        >
          <div class="result-title">
            <HighlightedText
              :text="item.title"
              :keyword="routeQueryKeyword"
              :ranges="getRanges(item, 'title')"
            />
          </div>
          <div class="result-meta">
            <el-tag :type="typeTagType(item.achievementType)" size="small">
              {{ typeLabel(item.achievementType) }}
            </el-tag>
            <el-tag :type="statusTagType(item.status)" size="small">
              {{ statusLabel(item.status) }}
            </el-tag>
            <span class="result-dept">{{ item.deptName }}</span>
            <span v-if="item.publishYear" class="result-year">{{ item.publishYear }}</span>
            <span class="result-score">相关度: {{ (item.relevanceScore || 0).toFixed(2) }}</span>
          </div>
          <div v-if="item.authors" class="result-authors">
            <HighlightedText
              :text="item.authors"
              :keyword="routeQueryKeyword"
              :ranges="getRanges(item, 'authors')"
            />
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <el-pagination
        v-if="total > pageSize"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="fetchResults"
        @current-change="fetchResults"
        style="margin-top: 16px; justify-content: center;"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { search, type SearchResultVO, type HighlightRange } from '@/api/search'
import { useUserStore } from '@/store/user'
import HighlightedText from '@/components/dashboard/HighlightedText.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── State ──────────────────────────────────────────────────────────
const results = ref<SearchResultVO[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const yearRange = ref<[string, string] | null>(null)

const filters = reactive({
  type: '',
  deptId: null as number | null,
  yearFrom: null as number | null,
  yearTo: null as number | null,
  classification: '',
})

// ── Computed ───────────────────────────────────────────────────────
const routeQueryKeyword = computed(() => {
  return (route.query.keyword as string) || ''
})

const departments = computed(() => userStore.deptList || [])

const isClassifiedManager = computed(() => {
  return userStore.roles?.includes('CLASSIFIED_MANAGER')
})

// ── Methods ────────────────────────────────────────────────────────
async function fetchResults() {
  const keyword = routeQueryKeyword.value
  if (!keyword) return

  loading.value = true
  error.value = false

  try {
    const params = {
      keyword,
      type: filters.type || undefined,
      deptId: filters.deptId ?? undefined,
      yearFrom: filters.yearFrom ?? undefined,
      yearTo: filters.yearTo ?? undefined,
      classification: (filters.classification || undefined) as 'NORMAL' | 'CLASSIFIED' | undefined,
      page: currentPage.value,
      size: pageSize.value,
    }

    const res = await search(params)
    const data = res.data as { records: SearchResultVO[]; total: number; page: number; pageSize: number } | undefined
    if (data) {
      results.value = data.records || []
      total.value = data.total || 0
    } else {
      results.value = []
      total.value = 0
    }
  } catch (e) {
    console.error('Search failed:', e)
    error.value = true
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function goToDetail(item: SearchResultVO) {
  router.push({
    path: `/achievement/detail/${item.id}`,
    query: { type: item.achievementType },
  })
}

function getRanges(item: SearchResultVO, field: 'title' | 'authors'): HighlightRange[] {
  return (item.highlightRanges || []).filter((r) => r.field === field)
}

function handleYearChange(val: [string, string] | null) {
  if (val && val.length === 2) {
    filters.yearFrom = parseInt(val[0], 10)
    filters.yearTo = parseInt(val[1], 10)
  } else {
    filters.yearFrom = null
    filters.yearTo = null
  }
  currentPage.value = 1
  fetchResults()
}

function typeLabel(type: string): string {
  const map: Record<string, string> = {
    paper: '论文',
    patent: '专利',
    software: '软著',
  }
  return map[type] || type
}

function typeTagType(type: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning'> = {
    paper: '',
    patent: 'success',
    software: 'warning',
  }
  return map[type] || ''
}

function statusLabel(status: string): string {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    PUBLISHED: '已发表',
    GRANTED: '已授权',
    INVALIDATED: '已失效',
    SUBMITTED: '已提交',
    VALID: '有效',
    INVALID: '无效',
  }
  return map[status] || status
}

function statusTagType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    DRAFT: 'info',
    PENDING: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    PUBLISHED: 'success',
    GRANTED: 'success',
    INVALIDATED: 'danger',
    SUBMITTED: 'warning',
    VALID: 'success',
    INVALID: 'danger',
  }
  return map[status] || 'info'
}

// ── Lifecycle & Watchers ───────────────────────────────────────────
onMounted(() => {
  if (routeQueryKeyword.value) {
    fetchResults()
  }
})

watch(
  () => route.query.keyword,
  () => {
    if (routeQueryKeyword.value) {
      currentPage.value = 1
      fetchResults()
    }
  }
)
</script>

<style scoped lang="scss">
.search-results-page {
  max-width: 960px;
  margin: 0 auto;
}

// ── Search Info ────────────────────────────────────────────────────
.search-info {
  margin-bottom: 16px;
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.search-info-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.search-info-count {
  font-size: 14px;
  color: #909399;
}

// ── Filter Bar ─────────────────────────────────────────────────────
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: #606266;
  white-space: nowrap;
}

// ── Result List ────────────────────────────────────────────────────
.results-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.result-item {
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
  }
}

.result-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.5;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}

.result-dept {
  color: #606266;
}

.result-year {
  color: #909399;
}

.result-score {
  color: #909399;
  font-size: 12px;
}

.result-authors {
  margin-top: 4px;
  font-size: 13px;
  color: #909399;
}
</style>
