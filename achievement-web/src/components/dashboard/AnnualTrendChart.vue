<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">年度成果数量趋势</span>
      <el-space>
        <el-button size="small" :loading="exportLoading" @click="handleExportExcel">
          <el-icon><Download /></el-icon> 导出Excel
        </el-button>
        <el-button size="small" :loading="pdfLoading" @click="handleExportPdf">
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

const exportLoading = ref(false)
const pdfLoading = ref(false)

type YearData = Record<number, { paper: number; patent: number; software: number }>

const chartOption = computed(() => {
  if (!props.data || props.data.length === 0) {
    return {
      title: {
        text: '暂无统计数据',
        left: 'center',
        top: 'center',
        textStyle: { fontSize: 14, color: '#909399' },
      },
    }
  }

  // Group data by year
  const years = [...new Set(props.data.map((d) => d.year))].sort()
  const yearMap: YearData = {}
  for (const year of years) {
    yearMap[year] = { paper: 0, patent: 0, software: 0 }
  }
  for (const d of props.data) {
    if (yearMap[d.year]) {
      yearMap[d.year][d.achievementType] = d.count
    }
  }

  return {
    tooltip: { trigger: 'axis' },
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
      type: 'category',
      data: years,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      name: '数量',
      axisLine: { show: false },
      splitLine: { lineStyle: { color: '#e4e7ed' } },
    },
    series: [
      {
        name: '论文',
        type: 'line',
        smooth: true,
        data: years.map((y) => yearMap[y].paper),
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#409eff' },
      },
      {
        name: '专利',
        type: 'line',
        smooth: true,
        data: years.map((y) => yearMap[y].patent),
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#67c23a' },
      },
      {
        name: '软著',
        type: 'line',
        smooth: true,
        data: years.map((y) => yearMap[y].software),
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#e6a23c' },
      },
    ],
    color: ['#409eff', '#67c23a', '#e6a23c'],
  }
})

async function handleExportExcel() {
  exportLoading.value = true
  try {
    const res: any = await dashboardApi.exportExcel('trend')
    const blob = res instanceof Blob ? res : res?.data
    if (!blob) { ElMessage.error('导出失败'); return }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'annual-trend.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exportLoading.value = false
  }
}

async function handleExportPdf() {
  pdfLoading.value = true
  try {
    const res: any = await dashboardApi.exportPdf('trend')
    const blob = res instanceof Blob ? res : res?.data
    if (!blob) { ElMessage.error('导出失败'); return }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'annual-trend.pdf'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    pdfLoading.value = false
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
  font-size: 15px;
  font-weight: 600;
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
