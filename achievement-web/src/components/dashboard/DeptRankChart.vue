<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">部门成果排行</span>
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
import { BarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { Download, Document } from '@element-plus/icons-vue'
import * as dashboardApi from '@/api/dashboard'
import type { DashboardDeptRankVO } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, BarChart, TooltipComponent, LegendComponent, GridComponent])

const props = defineProps<{
  data: DashboardDeptRankVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  'export-excel': []
  'export-pdf': []
}>()

const exportLoading = ref(false)
const pdfLoading = ref(false)

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

  const deptNames = props.data.map((d) => d.deptName)

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
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
      data: deptNames,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { rotate: deptNames.length > 5 ? 30 : 0 },
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
        type: 'bar',
        stack: 'total',
        data: props.data.map((d) => d.paperCount),
        itemStyle: { color: '#409eff' },
      },
      {
        name: '专利',
        type: 'bar',
        stack: 'total',
        data: props.data.map((d) => d.patentCount),
        itemStyle: { color: '#67c23a' },
      },
      {
        name: '软著',
        type: 'bar',
        stack: 'total',
        data: props.data.map((d) => d.softwareCount),
        itemStyle: { color: '#e6a23c' },
      },
    ],
    color: ['#409eff', '#67c23a', '#e6a23c'],
  }
})

async function handleExportExcel() {
  exportLoading.value = true
  try {
    const res: any = await dashboardApi.exportExcel('dept-rank')
    const blob = res instanceof Blob ? res : res?.data
    if (!blob) { ElMessage.error('导出失败'); return }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'dept-rank.xlsx'
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
    const res: any = await dashboardApi.exportPdf('dept-rank')
    const blob = res instanceof Blob ? res : res?.data
    if (!blob) { ElMessage.error('导出失败'); return }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'dept-rank.pdf'
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
