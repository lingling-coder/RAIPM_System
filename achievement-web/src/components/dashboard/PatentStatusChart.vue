<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">专利有效/失效占比</span>
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

const exportLoading = ref(false)
const pdfLoading = ref(false)

const STATUS_COLORS: Record<string, string> = {
  VALID: '#67c23a',
  INVALID: '#f56c6c',
  UNKNOWN: '#909399',
}

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

  const total = props.data.reduce((sum, d) => sum + d.count, 0)
  const pieData = props.data.map((d) => ({
    name: d.label,
    value: d.count,
    itemStyle: { color: STATUS_COLORS[d.status] || '#909399' },
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: true,
        label: {
          show: true,
          formatter: '{b}: {d}%',
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
        },
        labelLine: { show: true },
        data: pieData,
      },
    ],
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: '42%',
        style: {
          text: `总计\n${total}`,
          textAlign: 'center',
          fill: '#303133',
          fontSize: 18,
          fontWeight: 700,
          lineHeight: 28,
        },
      },
    ],
    color: ['#67c23a', '#f56c6c', '#909399'],
  }
})

async function handleExportExcel() {
  exportLoading.value = true
  try {
    const res: any = await dashboardApi.exportExcel('patent-status')
    const blob = res instanceof Blob ? res : res?.data
    if (!blob) { ElMessage.error('导出失败'); return }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'patent-status.xlsx'
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
    const res: any = await dashboardApi.exportPdf('patent-status')
    const blob = res instanceof Blob ? res : res?.data
    if (!blob) { ElMessage.error('导出失败'); return }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = 'patent-status.pdf'
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
