<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">年度成果数量趋势</span>
      <el-space>
        <el-button size="small" :loading="exportExcelLoading" @click="handleExportExcel">
          <el-icon><Download /></el-icon> 导出Excel
        </el-button>
        <el-button size="small" :loading="exportPdfLoading" @click="handleExportPdf">
          <el-icon><Document /></el-icon> 导出PDF
        </el-button>
      </el-space>
    </div>
    <v-chart v-if="!loading" class="chart" :option="chartOption" autoresize />
    <el-skeleton v-else :rows="5" animated class="chart-skeleton" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { Download, Document } from '@element-plus/icons-vue'
import * as dashboardApi from '@/api/dashboard'
import type { DashboardTrendVO } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, LineChart, TooltipComponent, LegendComponent, GridComponent])

const props = defineProps<{
  data: DashboardTrendVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  'export-excel': []
  'export-pdf': []
}>()

const exportExcelLoading = ref(false)
const exportPdfLoading = ref(false)

const typeLabelMap: Record<string, string> = {
  paper: '论文',
  patent: '专利',
  software: '软著',
}

const typeOrder = ['paper', 'patent', 'software']
const typeColors = ['#409eff', '#67c23a', '#e6a23c']

const CHART_TITLE = '年度成果数量趋势'

const chartOption = computed(() => {
  if (!props.data || props.data.length === 0) {
    return {
      title: { text: CHART_TITLE, left: 'center', textStyle: { fontSize: 14, color: '#303133' } },
      graphic: {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无统计数据',
          fill: '#909399',
          fontSize: 14,
        },
      },
    }
  }

  const years = [...new Set(props.data.map((d) => d.year))].sort((a, b) => a - b)

  return {
    tooltip: { trigger: 'axis' as const },
    legend: {
      data: ['论文', '专利', '软著'],
      top: 0,
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true,
    },
    xAxis: {
      type: 'category' as const,
      data: years,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      splitLine: { lineStyle: { color: '#e4e7ed' } },
    },
    yAxis: {
      type: 'value' as const,
      name: '数量',
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      splitLine: { lineStyle: { color: '#e4e7ed' } },
    },
    series: typeOrder.map((type, idx) => ({
      name: typeLabelMap[type],
      type: 'line' as const,
      smooth: true,
      data: years.map((y) => {
        const item = props.data.find((d) => d.year === y && d.achievementType === type)
        return item ? item.count : 0
      }),
      areaStyle: { opacity: 0.15, color: typeColors[idx] },
      itemStyle: { color: typeColors[idx] },
      lineStyle: { color: typeColors[idx] },
    })),
    color: typeColors,
  }
})

async function handleExportExcel() {
  exportExcelLoading.value = true
  emit('export-excel')
  try {
    const res: any = await dashboardApi.exportExcel('trend')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'annual-trend.xlsx'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    } else {
      ElMessage.error('导出失败')
    }
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportExcelLoading.value = false
  }
}

async function handleExportPdf() {
  exportPdfLoading.value = true
  emit('export-pdf')
  try {
    const res: any = await dashboardApi.exportPdf('trend')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'annual-trend.pdf'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    } else {
      ElMessage.error('导出失败')
    }
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportPdfLoading.value = false
  }
}
</script>

<style scoped>
.chart-wrapper {
  width: 100%;
}
.chart-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.chart-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}
.chart {
  height: 420px;
  width: 100%;
}
.chart-skeleton {
  height: 420px;
}
</style>
