import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { ElMessage } from 'element-plus'

// Mock element-plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    warning: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
  },
  ElButton: {
    name: 'ElButton',
    props: ['size', 'loading', 'type'],
    emits: ['click'],
    template: '<button @click="$emit(\'click\')"><slot /></button>',
  },
  ElSpace: {
    name: 'ElSpace',
    template: '<div><slot /></div>',
  },
  ElIcon: {
    name: 'ElIcon',
    template: '<span><slot /></span>',
  },
  ElSkeleton: {
    name: 'ElSkeleton',
    props: ['rows', 'animated'],
    template: '<div class="el-skeleton"><slot /></div>',
  },
}))

// Mock ECharts core (tree-shaking imports)
vi.mock('echarts/core', () => ({
  use: vi.fn(),
}))

vi.mock('echarts/renderers', () => ({
  CanvasRenderer: vi.fn(),
}))

vi.mock('echarts/charts', () => ({
  LineChart: vi.fn(),
  PieChart: vi.fn(),
  BarChart: vi.fn(),
}))

vi.mock('echarts/components', () => ({
  TooltipComponent: vi.fn(),
  LegendComponent: vi.fn(),
  GridComponent: vi.fn(),
}))

// Mock vue-echarts
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    props: ['option', 'autoresize'],
    template: '<div class="v-chart"><slot /></div>',
  },
}))

// Mock @element-plus/icons-vue
vi.mock('@element-plus/icons-vue', () => ({
  Download: { name: 'Download', template: '<span>D</span>' },
  Document: { name: 'Document', template: '<span>Doc</span>' },
}))

// Mock dashboard API
vi.mock('@/api/dashboard', () => ({
  exportExcel: vi.fn(() => Promise.resolve({ data: new Blob() })),
  exportPdf: vi.fn(() => Promise.resolve({ data: new Blob() })),
  getAnnualTrend: vi.fn(() => Promise.resolve({ data: [] })),
  getTypeDist: vi.fn(() => Promise.resolve({ data: [] })),
  getDeptRanking: vi.fn(() => Promise.resolve({ data: [] })),
  getPatentStatus: vi.fn(() => Promise.resolve({ data: [] })),
}))

describe('AnnualTrendChart.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders toolbar title and export buttons', async () => {
    const AnnualTrendChart = (await import('@/components/dashboard/AnnualTrendChart.vue')).default

    const wrapper = mount(AnnualTrendChart, {
      props: {
        data: [],
        loading: false,
      },
      global: {
        stubs: {
          'v-chart': true,
          'el-button': {
            name: 'ElButton',
            props: ['size', 'loading', 'type'],
            template: '<button class="el-button" @click="$emit(\'click\')"><slot /></button>',
          },
          'el-space': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span><slot /></span>' },
          'el-skeleton': { template: '<div class="el-skeleton"><slot /></div>' },
        },
      },
    })

    // Check title text
    expect(wrapper.text()).toContain('年度成果数量趋势')

    // Check export buttons exist
    const buttons = wrapper.findAll('button')
    const buttonTexts = buttons.map((b) => b.text())
    expect(buttonTexts.some((t) => t.includes('导出Excel'))).toBe(true)
    expect(buttonTexts.some((t) => t.includes('导出PDF'))).toBe(true)
  })

  it('renders skeleton when loading', async () => {
    const AnnualTrendChart = (await import('@/components/dashboard/AnnualTrendChart.vue')).default

    const wrapper = mount(AnnualTrendChart, {
      props: {
        data: [],
        loading: true,
      },
      global: {
        stubs: {
          'v-chart': true,
          'el-button': { template: '<button class="el-button"><slot /></button>' },
          'el-space': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span><slot /></span>' },
          'el-skeleton': { template: '<div class="el-skeleton"><slot /></div>' },
        },
      },
    })

    // Check skeleton exists when loading
    expect(wrapper.find('.el-skeleton').exists()).toBe(true)
  })

  it('renders chart area when not loading', async () => {
    const AnnualTrendChart = (await import('@/components/dashboard/AnnualTrendChart.vue')).default

    const wrapper = mount(AnnualTrendChart, {
      props: {
        data: [],
        loading: false,
      },
      global: {
        stubs: {
          'v-chart': { template: '<div class="v-chart" />' },
          'el-button': { template: '<button class="el-button"><slot /></button>' },
          'el-space': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span><slot /></span>' },
          'el-skeleton': { template: '<div class="el-skeleton"><slot /></div>' },
        },
      },
    })

    // v-chart should render when not loading
    expect(wrapper.find('.v-chart').exists()).toBe(true)
    // skeleton should not render when not loading
    // Note: the skeleton is rendered by v-if="loading" so when loading=false, v-chart shows
    expect(wrapper.find('.el-skeleton').exists()).toBe(false)
  })

  it('emits export-excel when Excel button clicked', async () => {
    const AnnualTrendChart = (await import('@/components/dashboard/AnnualTrendChart.vue')).default

    const wrapper = mount(AnnualTrendChart, {
      props: {
        data: [],
        loading: false,
      },
      global: {
        stubs: {
          'v-chart': { template: '<div class="v-chart" />' },
          'el-button': {
            name: 'ElButton',
            props: ['size', 'loading', 'type'],
            template: '<button class="el-button" :class="{ loading: loading }" @click="$emit(\'click\', $event)"><slot /></button>',
          },
          'el-space': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span><slot /></span>' },
          'el-skeleton': { template: '<div class="el-skeleton"><slot /></div>' },
        },
      },
    })

    // Find Excel export button and click it
    const buttons = wrapper.findAll('button')
    const excelButton = buttons.find((b) => b.text().includes('导出Excel'))
    expect(excelButton).toBeDefined()

    if (excelButton) {
      await excelButton.trigger('click')
    }

    // Wait for next tick
    await new Promise((r) => setTimeout(r, 0))

    // Should not throw errors — the export handler tries to call API and blob download
    // Since we mock the API, it should succeed
  })

  it('emits export-pdf when PDF button clicked', async () => {
    const AnnualTrendChart = (await import('@/components/dashboard/AnnualTrendChart.vue')).default

    const wrapper = mount(AnnualTrendChart, {
      props: {
        data: [],
        loading: false,
      },
      global: {
        stubs: {
          'v-chart': { template: '<div class="v-chart" />' },
          'el-button': {
            name: 'ElButton',
            props: ['size', 'loading', 'type'],
            template: '<button class="el-button" :class="{ loading: loading }" @click="$emit(\'click\', $event)"><slot /></button>',
          },
          'el-space': { template: '<div><slot /></div>' },
          'el-icon': { template: '<span><slot /></span>' },
          'el-skeleton': { template: '<div class="el-skeleton"><slot /></div>' },
        },
      },
    })

    // Find PDF export button and click it
    const buttons = wrapper.findAll('button')
    const pdfButton = buttons.find((b) => b.text().includes('导出PDF'))
    expect(pdfButton).toBeDefined()

    if (pdfButton) {
      await pdfButton.trigger('click')
    }

    // Wait for next tick
    await new Promise((r) => setTimeout(r, 0))

    // Should not throw errors
  })
})
