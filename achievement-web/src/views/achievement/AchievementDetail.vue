<template>
  <div class="achievement-detail" v-loading="loading">
    <el-breadcrumb>
      <el-breadcrumb-item :to="{ path: '/achievement/list' }">成果列表</el-breadcrumb-item>
      <el-breadcrumb-item>成果详情</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Header -->
    <div class="detail-header" v-if="detail">
      <div class="header-left">
        <span class="title-text">{{ displayTitle }}</span>
        <el-tag v-if="detail.status" :type="statusTagType(detail.status)">{{ detail.statusLabel }}</el-tag>
        <el-tag v-if="detail.isClassified" type="warning" effect="dark">
          {{ detail.classifiedLevel || '涉密' }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-tag type="info">{{ achievementTypeLabel }}</el-tag>
      </div>
    </div>

    <!-- Action bar (dynamic per status per D-40) -->
    <div class="action-bar" v-if="detail">
      <!-- DRAFT: Edit -->
      <el-button v-if="detail.status === 'DRAFT'" type="primary" @click="editAchievement">
        编辑
      </el-button>

      <!-- PENDING: Withdraw (submitter only per D-29) -->
      <el-button
        v-if="detail.status === 'PENDING_DEPT_REVIEW' || detail.status === 'PENDING_ADMIN_ARCHIVE'"
        type="warning"
        :loading="withdrawLoading"
        @click="handleWithdraw"
      >
        撤回
      </el-button>

      <!-- REJECTED: Edit and resubmit -->
      <el-button v-if="detail.status === 'REJECTED'" type="primary" @click="editAchievement">
        编辑
      </el-button>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" v-if="detail">
      <!-- Tab: Basic Info -->
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="1" border class="detail-descriptions">
          <!-- Paper fields -->
          <template v-if="achievementType === 'paper'">
            <el-descriptions-item label="论文标题">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="作者">{{ detail.authors }}</el-descriptions-item>
            <el-descriptions-item label="期刊/会议">{{ detail.journal }}</el-descriptions-item>
            <el-descriptions-item label="DOI">{{ detail.doi || '—' }}</el-descriptions-item>
            <el-descriptions-item label="ISSN/CN">{{ detail.issn || '—' }}</el-descriptions-item>
            <el-descriptions-item label="卷号">{{ detail.volume || '—' }}</el-descriptions-item>
            <el-descriptions-item label="期号">{{ detail.issue || '—' }}</el-descriptions-item>
            <el-descriptions-item label="页码">{{ detail.pages || '—' }}</el-descriptions-item>
            <el-descriptions-item label="发表年份">{{ detail.publishYear }}</el-descriptions-item>
            <el-descriptions-item label="收录情况">{{ detail.indexStatus }}</el-descriptions-item>
            <el-descriptions-item label="影响因子">{{ detail.impactFactor ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="中科院分区">{{ detail.zone || '—' }}</el-descriptions-item>
            <el-descriptions-item label="摘要">{{ detail.abstractText || '—' }}</el-descriptions-item>
          </template>

          <!-- Patent fields -->
          <template v-if="achievementType === 'patent'">
            <el-descriptions-item label="专利名称">{{ detail.patentName }}</el-descriptions-item>
            <el-descriptions-item label="发明人">{{ detail.inventors }}</el-descriptions-item>
            <el-descriptions-item label="申请号">{{ detail.applicationNo }}</el-descriptions-item>
            <el-descriptions-item label="授权号">{{ detail.authorizationNo || '—' }}</el-descriptions-item>
            <el-descriptions-item label="申请日">{{ detail.applicationDate }}</el-descriptions-item>
            <el-descriptions-item label="授权日">{{ detail.authorizationDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="专利类型">{{ detail.patentType }}</el-descriptions-item>
            <el-descriptions-item label="国别">{{ detail.country }}</el-descriptions-item>
            <el-descriptions-item label="年费下次缴费日">{{ detail.nextFeeDate || '—' }}</el-descriptions-item>
            <el-descriptions-item label="法律状态">{{ detail.legalStatus }}</el-descriptions-item>
          </template>

          <!-- Copyright fields -->
          <template v-if="achievementType === 'copyright'">
            <el-descriptions-item label="软著名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="著作权人">{{ detail.copyrightHolder }}</el-descriptions-item>
            <el-descriptions-item label="登记号">{{ detail.registrationNo }}</el-descriptions-item>
            <el-descriptions-item label="登记日期">{{ detail.registrationDate }}</el-descriptions-item>
            <el-descriptions-item label="版本号">{{ detail.softwareVersion }}</el-descriptions-item>
            <el-descriptions-item label="软件类别">{{ detail.softwareCategory }}</el-descriptions-item>
          </template>

          <!-- Common fields for all types -->
          <el-descriptions-item label="涉密标记">{{ detail.isClassified ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.isClassified" label="密级">{{ detail.classifiedLevel }}</el-descriptions-item>
          <el-descriptions-item label="所属课题">{{ detail.projectRef || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ detail.statusLabel }}</el-descriptions-item>
          <el-descriptions-item label="归档号">{{ detail.archiveNo || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab: Approval Progress -->
      <el-tab-pane label="审批进度" name="approval">
        <AchievementTimeline
          :records="approvalHistory"
          :show-pending="isPending"
        />
      </el-tab-pane>

      <!-- Tab: Attachments -->
      <el-tab-pane label="附件" name="attachments">
        <el-empty description="暂无附件" v-if="attachments.length === 0">
          <el-button type="primary" @click="router.push(`/achievement/register?draftId=${detail?.id}`)">上传附件</el-button>
        </el-empty>
        <el-table v-else :data="attachments">
          <el-table-column prop="originalName" label="文件名" />
          <el-table-column prop="fileSizeDisplay" label="大小" />
          <el-table-column prop="uploadTime" label="上传时间" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="downloadAttachment(scope.row)">下载</el-button>
              <el-button v-if="detail?.status === 'DRAFT' || detail?.status === 'REJECTED'"
                size="small" type="danger" @click="deleteAttachment(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- Tab: Operation Log -->
      <el-tab-pane label="操作日志" name="log">
        <el-empty description="暂无操作日志" v-if="operationLogs.length === 0" />
        <el-table v-else :data="operationLogs">
          <el-table-column prop="createdTime" label="时间" width="160" />
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column prop="operationName" label="操作" min-width="200" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as paperApi from '@/api/achievement/paper'
import * as patentApi from '@/api/achievement/patent'
import * as copyrightApi from '@/api/achievement/copyright'
import * as approvalApi from '@/api/approval'
import * as attachmentApi from '@/api/attachment'
import AchievementTimeline from '@/components/achievement/AchievementTimeline.vue'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const withdrawLoading = ref(false)
const detail = ref<any>(null)
const achievementType = ref<'paper' | 'patent' | 'copyright' | null>(null)
const activeTab = ref('basic')
const attachments = ref<any[]>([])
const approvalHistory = ref<any[]>([])
const operationLogs = ref<any[]>([])

const isPending = computed(() =>
  detail.value?.status === 'PENDING_DEPT_REVIEW' || detail.value?.status === 'PENDING_ADMIN_ARCHIVE'
)

// Determine achievement type from response data structure
function detectType(data: any): 'paper' | 'patent' | 'copyright' {
  if (data.title !== undefined) return 'paper'
  if (data.patentName !== undefined) return 'patent'
  if (data.name !== undefined) return 'copyright'
  return 'paper'
}

const displayTitle = computed(() => {
  if (!detail.value) return ''
  if (achievementType.value === 'paper') return detail.value.title
  if (achievementType.value === 'patent') return detail.value.patentName
  if (achievementType.value === 'copyright') return detail.value.name
  return ''
})

const achievementTypeLabel = computed(() => {
  const labels: Record<string, string> = {
    paper: '论文',
    patent: '专利',
    copyright: '软件著作权',
  }
  return achievementType.value ? labels[achievementType.value] : '未知'
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    let data: any = null

    try {
      const paperRes: any = await paperApi.getById(id)
      if (paperRes?.data) data = paperRes.data
    } catch { /* not a paper */ }

    if (!data) {
      try {
        const patentRes: any = await patentApi.getById(id)
        if (patentRes?.data) data = patentRes.data
      } catch { /* not a patent */ }
    }

    if (!data) {
      try {
        const copyrightRes: any = await copyrightApi.getById(id)
        if (copyrightRes?.data) data = copyrightRes.data
      } catch { /* not a copyright */ }
    }

    if (data) {
      detail.value = data
      achievementType.value = detectType(data)

      // Load approval history
      if (achievementType.value) {
        try {
          const histRes: any = await approvalApi.getHistory(achievementType.value, id)
          if (histRes?.data) {
            approvalHistory.value = histRes.data
          }
        } catch { /* history may not exist */ }
      }
    }

    // Load attachments
    try {
      const attRes: any = await attachmentApi.getAttachments(
        achievementType.value || 'paper', id)
      if (attRes?.data) {
        attachments.value = attRes.data
      }
    } catch { /* attachments may not be available */ }
  } catch {
    ElMessage.error('加载成果详情失败')
  } finally {
    loading.value = false
  }
})

function statusTagType(status: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING_DEPT_REVIEW: 'warning',
    PENDING_ADMIN_ARCHIVE: 'primary',
    ARCHIVED: 'success',
    REJECTED: 'danger',
    INVALIDATED: 'info',
    WITHDRAWN: 'info',
  }
  return map[status] || 'info'
}

function editAchievement() {
  router.push(`/achievement/register?draftId=${detail.value?.id}`)
}

function downloadAttachment(row: any) {
  if (row?.id) {
    attachmentApi.download(row.id)
  }
}

async function deleteAttachment(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该附件？此操作不可撤销。', '确认删除', { type: 'warning' })
    await attachmentApi.deleteAttachment(row.id)
    attachments.value = attachments.value.filter((a: any) => a.id !== row.id)
    ElMessage.success('附件已删除')
  } catch { /* cancelled */ }
}

async function handleWithdraw() {
  if (!achievementType.value || !detail.value?.id) return
  try {
    await ElMessageBox.confirm(
      '确认撤回该审批申请？撤回后成果状态恢复为草稿。',
      '确认撤回',
      { type: 'info', confirmButtonText: '确认撤回', cancelButtonText: '取消' }
    )
    withdrawLoading.value = true
    const res: any = await approvalApi.withdraw(achievementType.value, detail.value.id)
    if (res?.code === 200) {
      ElMessage.info('已撤回，成果恢复为草稿')
      detail.value.status = 'WITHDRAWN'
      if (detail.value.statusLabel !== undefined) {
        detail.value.statusLabel = '已撤回'
      }
    }
  } catch { /* cancelled or failed */ }
  finally {
    withdrawLoading.value = false
  }
}
</script>

<style scoped>
.achievement-detail {
  width: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
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

.action-bar {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}

.detail-descriptions {
  margin-top: 16px;
}
</style>
