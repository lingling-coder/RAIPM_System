import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { type Ref, ref } from 'vue'
import { createPinia, setActivePinia } from 'pinia'

// ── Mock search API ────────────────────────────────────────────────
const mockSearch = vi.fn()
vi.mock('@/api/search', () => ({
  search: mockSearch,
  SearchResultVO: {},
  HighlightRange: {},
}))

// ── Mock vue-router ────────────────────────────────────────────────
const pushMock = vi.fn()
const routeQueryRef: Ref<Record<string, string>> = ref({ keyword: 'test' })

// Use importOriginal to preserve other exports (createRouter, etc.)
// that are needed by router/index.ts which is imported by store/user.ts
vi.mock('vue-router', async (importOriginal: any) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRoute: () => ({
      query: routeQueryRef.value,
      params: {},
    }),
    useRouter: () => ({ push: pushMock }),
  }
})

// ── Mock HighlightedText (tested separately) ───────────────────────
vi.mock('@/components/dashboard/HighlightedText.vue', () => ({
  default: {
    name: 'HighlightedText',
    template: '<span class="mock-highlighted"><slot /><slot name="default">{{ text }}</slot></span>',
    props: ['text', 'keyword', 'ranges'],
  },
}))

// ── Sample search results ──────────────────────────────────────────
const makeResult = (id: number, overrides: Record<string, unknown> = {}) => ({
  id,
  title: `测试成果标题 ${id}`,
  achievementType: 'paper' as const,
  status: 'APPROVED',
  deptName: '计算机学院',
  authors: '作者 A, 作者 B',
  publishYear: 2025,
  relevanceScore: 8.5,
  highlightRanges: [
    { field: 'title', start: 0, end: 2 },
    { field: 'authors', start: 0, end: 2 },
  ],
  ...overrides,
})

describe('SearchResults.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    routeQueryRef.value = { keyword: 'test' }
    mockSearch.mockResolvedValue({
      data: { records: [], total: 0, page: 1, pageSize: 10 },
    })
  })

  it('renders empty state when no results', async () => {
    mockSearch.mockResolvedValue({
      data: { records: [], total: 0, page: 1, pageSize: 10 },
    })

    const wrapper = mount((await import('@/views/search/SearchResults.vue')).default, {
      global: {
        stubs: {
          'el-select': false,
          'el-option': false,
          'el-date-picker': false,
          'el-skeleton': true,
          'el-alert': true,
          'el-empty': true,
          'el-pagination': true,
          'el-button': false,
          'el-tag': false,
        },
      },
    })

    // Wait for onMounted to call fetchResults
    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // Verify search API was called
    expect(mockSearch).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: 'test' })
    )

    // Should show empty state
    const emptyEl = wrapper.findComponent({ name: 'ElEmpty' })
    expect(emptyEl.exists()).toBe(true)
  })

  it('renders result items', async () => {
    mockSearch.mockResolvedValue({
      data: {
        records: [
          makeResult(1),
          makeResult(2, { achievementType: 'patent', status: 'PENDING' }),
        ],
        total: 2,
        page: 1,
        pageSize: 10,
      },
    })

    const wrapper = mount((await import('@/views/search/SearchResults.vue')).default, {
      global: {
        stubs: {
          'el-select': false,
          'el-option': false,
          'el-date-picker': false,
          'el-skeleton': true,
          'el-alert': true,
          'el-empty': true,
          'el-pagination': true,
          'el-button': false,
          'el-tag': true,
        },
      },
    })

    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // Should render result items
    const items = wrapper.findAll('.result-item')
    expect(items.length).toBe(2)

    // Should show total count
    expect(wrapper.text()).toContain('共 2 条结果')
  })

  it('filter change triggers re-fetch', async () => {
    mockSearch.mockResolvedValue({
      data: { records: [], total: 0, page: 1, pageSize: 10 },
    })

    const wrapper = mount((await import('@/views/search/SearchResults.vue')).default, {
      global: {
        stubs: {
          'el-select': false,
          'el-option': false,
          'el-date-picker': true,
          'el-skeleton': true,
          'el-alert': true,
          'el-empty': true,
          'el-pagination': true,
          'el-button': false,
          'el-tag': true,
        },
      },
    })

    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()
    expect(mockSearch).toHaveBeenCalledTimes(1)

    // Change type filter and call fetchResults directly (simulating @change on el-select)
    // Element Plus el-select @change is complex to simulate in vue-test-utils,
    // so we test the logic path: filter value change -> fetchResults with new filter
    const vm = wrapper.vm as any
    vm.filters.type = 'paper'
    // Call fetchResults directly as the @change handler would
    await vm.fetchResults()
    await wrapper.vm.$nextTick()

    expect(mockSearch).toHaveBeenCalledTimes(2)
    expect(mockSearch).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ keyword: 'test', type: 'paper' })
    )
  })

  it('click result navigates to detail page', async () => {
    mockSearch.mockResolvedValue({
      data: {
        records: [makeResult(42)],
        total: 1,
        page: 1,
        pageSize: 10,
      },
    })

    const wrapper = mount((await import('@/views/search/SearchResults.vue')).default, {
      global: {
        stubs: {
          'el-select': false,
          'el-option': false,
          'el-date-picker': true,
          'el-skeleton': true,
          'el-alert': true,
          'el-empty': true,
          'el-pagination': true,
          'el-button': false,
          'el-tag': true,
        },
      },
    })

    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // Click the first result item
    const resultItem = wrapper.find('.result-item')
    expect(resultItem.exists()).toBe(true)
    await resultItem.trigger('click')

    // Verify router.push was called
    expect(pushMock).toHaveBeenCalledWith({
      path: '/achievement/detail/42',
      query: { type: 'paper' },
    })
  })

  it('classification filter hidden for non-manager', async () => {
    // Ensure user has no CLASSIFIED_MANAGER role
    const userStore = (await import('@/store/user')).useUserStore()
    userStore.roles = []

    mockSearch.mockResolvedValue({
      data: { records: [], total: 0, page: 1, pageSize: 10 },
    })

    const wrapper = mount((await import('@/views/search/SearchResults.vue')).default, {
      global: {
        stubs: {
          'el-select': false,
          'el-option': false,
          'el-date-picker': true,
          'el-skeleton': true,
          'el-alert': true,
          'el-empty': true,
          'el-pagination': true,
          'el-button': false,
          'el-tag': true,
        },
      },
    })

    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // The filter bar should have 3 selects (type, dept, date) but NOT the classification one
    // We check by finding the placeholder text
    const filterText = wrapper.find('.filter-bar').text()
    expect(filterText).not.toContain('密级')
  })

  it('classification filter visible for classified manager', async () => {
    // Set user roles to include CLASSIFIED_MANAGER
    const userStore = (await import('@/store/user')).useUserStore()
    userStore.roles = ['CLASSIFIED_MANAGER']

    mockSearch.mockResolvedValue({
      data: { records: [], total: 0, page: 1, pageSize: 10 },
    })

    const wrapper = mount((await import('@/views/search/SearchResults.vue')).default, {
      global: {
        stubs: {
          'el-select': false,
          'el-option': false,
          'el-date-picker': true,
          'el-skeleton': true,
          'el-alert': true,
          'el-empty': true,
          'el-pagination': true,
          'el-button': false,
          'el-tag': true,
        },
      },
    })

    await new Promise((r) => setTimeout(r, 50))
    await wrapper.vm.$nextTick()

    // The filter bar should contain the classification select with "密级" placeholder
    const filterText = wrapper.find('.filter-bar').text()
    expect(filterText).toContain('密级')
  })
})
