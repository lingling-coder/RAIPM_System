<template>
  <div class="chart-wrapper">
    <div class="chart-toolbar">
      <span class="chart-title">成果类型分布</span>
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
import type { DashboardTypeDistVO } from '@/api/dashboard'
import { ElMessage } from 'element-plus'

use([CanvasRenderer, PieChart, TooltipComponent, LegendComponent])

const props = defineProps<{
  data: DashboardTypeDistVO[]
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

const typeColors = ['#409eff', '#67c23a', '#e6a23c']

const CHART_TITLE = '成果类型分布'

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

  const pieData = props.data.map((d) => ({
    name: typeLabelMap[d.achievementType] || d.achievementType,
    value: d.count,
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
    series: [
      {
        type: 'pie' as const,
        radius: ['0%', '65%'],
        center: ['40%', '50%'],
        label: {
          formatter: '{b}: {d}%',
        },
        data: pieData,
        itemStyle: {
          borderRadius: 4,
        },
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.15)',
          },
        },
      },
    ],
    color: typeColors,
  }
})

async function handleExportExcel() {
  exportExcelLoading.value = true
  emit('export-excel')
  try {
    const res: any = await dashboardApi.exportExcel('type-dist')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'type-dist.xlsx'
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
    const res: any = await dashboardApi.exportPdf('type-dist')
    const blob = res instanceof Blob ? res : res?.data
    if (blob) {
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'type-dist.pdf'
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
