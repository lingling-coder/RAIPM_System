<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">部门成果排行</span>
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

const exportExcelLoading = ref(false)
const exportPdfLoading = ref(false)

const typeLabels = ['论文', '专利', '软著']
const typeFields: Array<keyof DashboardDeptRankVO> = ['paperCount', 'patentCount', 'softwareCount']
const typeColors = ['#409eff', '#67c23a', '#e6a23c']

const CHART_TITLE = '部门成果排行'

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

  const deptNames = props.data.map((d) => d.deptName)

  return {
    tooltip: {
      trigger: 'axis' as const,
      axisPointer: { type: 'shadow' as const },
    },
    legend: {
      data: typeLabels,
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
      data: deptNames,
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: {
        interval: 0,
        rotate: deptNames.length > 6 ? 30 : 0,
      },
    },
    yAxis: {
      type: 'value' as const,
      name: '成果数量',
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      splitLine: { lineStyle: { color: '#e4e7ed' } },
    },
    series: typeLabels.map((label, idx) => ({
      name: label,
      type: 'bar' as const,
      data: props.data.map((d) => Number(d[typeFields[idx]]) || 0),
      itemStyle: { color: typeColors[idx] },
    })),
    color: typeColors,
  }
})

async function handleExportExcel() {
  exportExcelLoading.value = true
  emit('export-excel')
  try {
    const res: any = await dashboardApi.exportExcel('dept-rank')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'dept-rank.xlsx'
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
    const res: any = await dashboardApi.exportPdf('dept-rank')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'dept-rank.pdf'
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
