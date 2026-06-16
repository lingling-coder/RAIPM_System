<template>
  <div class="fee-detail" v-loading="loading">
    <el-breadcrumb>
      <el-breadcrumb-item :to="{ path: '/fee/ledger' }">费用台账</el-breadcrumb-item>
      <el-breadcrumb-item>费用详情</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Header -->
    <div class="detail-header" v-if="detail">
      <div class="header-left">
        <span class="title-text">费用详情</span>
        <el-tag :type="feeTagType(detail.feeType)" size="small">{{ detail.feeTypeLabel }}</el-tag>
        <el-tag :type="statusTagType(detail.status)" size="small">{{ detail.statusLabel }}</el-tag>
      </div>
      <div class="header-right">
        <el-button @click="goBack">返回</el-button>
      </div>
    </div>

    <!-- Tabs (D-06) -->
    <el-tabs v-model="activeTab" v-if="detail">
      <!-- Tab: Basic Info -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="1" border class="detail-descriptions">
          <el-descriptions-item label="费用类型">{{ detail.feeTypeLabel || detail.feeType }}</el-descriptions-item>
          <el-descriptions-item label="关联成果">
            <el-link type="primary" :underline="false" @click="goToAchievement">
              {{ detail.ownerName || '—' }}
            </el-link>
          </el-descriptions-item>
          <el-descriptions-item label="截止日期">{{ detail.dueDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="金额">¥{{ detail.amount ? detail.amount.toFixed(2) : '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="实缴金额">
            <span v-if="detail.paidAmount != null">¥{{ detail.paidAmount.toFixed(2) }}</span>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="缴费日期">{{ detail.paidDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="缴费凭证号">{{ detail.voucherNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.statusLabel || detail.status }}</el-descriptions-item>
          <el-descriptions-item label="经费来源">{{ fundSourceLabel(detail.fundingSource) }}</el-descriptions-item>
          <el-descriptions-item label="缴费单编号">{{ detail.slipNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="记录来源">
            <el-tag :type="detail.source === 'auto_generated' ? 'info' : 'primary'" size="small">
              {{ detail.source === 'auto_generated' ? '自动生成' : '手动录入' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属部门ID">{{ detail.deptId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detail.createdTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detail.updatedTime || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab: Payment History (placeholder) -->
      <el-tab-pane label="缴费历史" name="history">
        <el-empty description="暂无缴费历史" />
      </el-tab-pane>

      <!-- Tab: Attachments (D-09) -->
      <el-tab-pane label="附件" name="attachments">
        <AttachmentUploader
          :achievement-type="detail.ownerType"
          :achievement-id="detail.ownerId"
          :is-classified="false"
          :show-delete="detail.status !== 'paid' && detail.status !== 'paused'"
        />
      </el-tab-pane>

      <!-- Tab: Operation Log (placeholder) -->
      <el-tab-pane label="操作日志" name="log">
        <el-empty description="暂无操作日志" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as feeApi from '@/api/fee/feeRecord'
import type { FeeRecordVO } from '@/api/fee/feeRecord'
import AttachmentUploader from '@/components/achievement/AttachmentUploader.vue'

type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<FeeRecordVO | null>(null)
const activeTab = ref('basic')

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res: any = await feeApi.getById(id)
    if (res?.data) {
      detail.value = res.data
    }
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
})

function goBack() {
  router.push('/fee/ledger')
}

function goToAchievement() {
  if (!detail.value) return
  if (detail.value.ownerType === 'patent' || detail.value.ownerType === 'copyright') {
    router.push(`/achievement/detail/${detail.value.ownerId}`)
  }
}

function feeTagType(feeType: string): TagType {
  const map: Record<string, TagType> = {
    annual_fee: 'primary',
    registration_fee: 'success',
    maintenance_fee: 'warning',
    other: 'info',
  }
  return map[feeType] || 'info'
}

function statusTagType(status: string): TagType {
  const map: Record<string, TagType> = {
    pending: 'warning',
    paid: 'success',
    paused: 'info',
  }
  return map[status] || 'info'
}

function fundSourceLabel(code: string | undefined): string {
  const map: Record<string, string> = {
    vertical: '纵向科研经费',
    horizontal: '横向科研经费',
    institute: '院配套',
    self: '自筹',
  }
  return code ? (map[code] || code) : '—'
}
</script>

<style scoped>
.fee-detail {
  width: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 16px 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.header-right {
  flex-shrink: 0;
}

.title-text {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.detail-descriptions {
  margin-top: 16px;
}
</style>
