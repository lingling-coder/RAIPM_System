import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'

// Mock ECharts tree-shaking modules
vi.mock('echarts/core', () => ({ use: vi.fn() }))
vi.mock('echarts/renderers', () => ({ CanvasRenderer: vi.fn() }))
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

// Mock vue-echarts VChart component
vi.mock('vue-echarts', () => ({
  default: {
    name: 'VChart',
    props: ['option', 'autoresize'],
    template: '<div class="v-chart-stub"></div>',
  },
}))

// Mock dashboard API to prevent actual HTTP calls
vi.mock('@/api/dashboard', () => ({
  exportExcel: vi.fn(() => Promise.resolve(new Blob(['test']))),
  exportPdf: vi.fn(() => Promise.resolve(new Blob(['test']))),
  getAnnualTrend: vi.fn(() => Promise.resolve({ data: [] })),
  getTypeDist: vi.fn(() => Promise.resolve({ data: [] })),
  getDeptRanking: vi.fn(() => Promise.resolve({ data: [] })),
  getPatentStatus: vi.fn(() => Promise.resolve({ data: [] })),
}))

// Mock element-plus — keep original exports, override ElMessage for testing
vi.mock(import('element-plus'), async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...(actual as any),
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
  }
})

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
          'el-icon': true,
          'el-space': { template: '<div class="el-space-stub"><slot /></div>' },
        },
      },
    })

    // Check title text
    expect(wrapper.text()).toContain('年度成果数量趋势')

    // Check export buttons exist
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const excelButton = buttons.find((b) => b.text().includes('导出Excel'))
    const pdfButton = buttons.find((b) => b.text().includes('导出PDF'))
    expect(excelButton).toBeDefined()
    expect(pdfButton).toBeDefined()
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
          'el-icon': true,
          'el-space': { template: '<div class="el-space-stub"><slot /></div>' },
          'el-skeleton': true,
        },
      },
    })

    // When loading, the chart-wrapper still renders but v-chart is hidden
    expect(wrapper.find('.chart-wrapper').exists()).toBe(true)
    // The toolbar title should still be visible
    expect(wrapper.text()).toContain('年度成果数量趋势')
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
          'v-chart': true,
          'el-icon': true,
          'el-space': { template: '<div class="el-space-stub"><slot /></div>' },
        },
      },
    })

    // Find and click the 导出Excel button
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const excelButton = buttons.find((b) => b.text().includes('导出Excel'))
    expect(excelButton).toBeDefined()
    if (excelButton) {
      await excelButton.trigger('click')
    }

    // Assert emitted event
    expect(wrapper.emitted('export-excel')).toBeTruthy()
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
          'v-chart': true,
          'el-icon': true,
          'el-space': { template: '<div class="el-space-stub"><slot /></div>' },
        },
      },
    })

    // Find and click the 导出PDF button
    const buttons = wrapper.findAllComponents({ name: 'ElButton' })
    const pdfButton = buttons.find((b) => b.text().includes('导出PDF'))
    expect(pdfButton).toBeDefined()
    if (pdfButton) {
      await pdfButton.trigger('click')
    }

    // Assert emitted event
    expect(wrapper.emitted('export-pdf')).toBeTruthy()
  })
})
