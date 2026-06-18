<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">专利有效状态</span>
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
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import VChart from 'vue-echarts'
import { Download, Document } from '@element-plus/icons-vue'
import * as dashboardApi from '@/api/dashboard'
import type { DashboardPatentStatusVO } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const props = defineProps<{
  data: DashboardPatentStatusVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  'export-excel': []
  'export-pdf': []
}>()

const exportExcelLoading = ref(false)
const exportPdfLoading = ref(false)

const statusColors: Record<string, string> = {
  VALID: '#67c23a',
  INVALID: '#f56c6c',
  UNKNOWN: '#909399',
}

const CHART_TITLE = '专利有效状态'

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

  const total = props.data.reduce((sum, d) => sum + d.count, 0)
  const pieData = props.data.map((d) => ({
    name: d.label,
    value: d.count,
    itemStyle: {
      color: statusColors[d.status] || '#909399',
    },
  }))

  return {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
    },
    graphic: {
      type: 'text',
      left: '33%',
      top: '45%',
      style: {
        text: `总计\n${total}`,
        textAlign: 'center',
        fill: '#303133',
        fontSize: 14,
        fontWeight: 700,
        lineHeight: 24,
      },
    },
    series: [
      {
        type: 'pie' as const,
        radius: ['45%', '70%'],
        center: ['40%', '50%'],
        label: {
          formatter: '{b}: {d}%',
        },
        data: pieData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.15)',
          },
        },
      },
    ],
  }
})

async function handleExportExcel() {
  exportExcelLoading.value = true
  emit('export-excel')
  try {
    const res: any = await dashboardApi.exportExcel('patent-status')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'patent-status.xlsx'
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
    const res: any = await dashboardApi.exportPdf('patent-status')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'patent-status.pdf'
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
