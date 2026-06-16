<template>
  <div class="approval-detail" v-loading="loading">
    <!-- Breadcrumb -->
    <el-breadcrumb>
      <el-breadcrumb-item :to="{ path: '/approval/pending' }">审批待办</el-breadcrumb-item>
      <el-breadcrumb-item>审批详情</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="split-layout" v-if="detail">
      <!-- Left Panel (scrollable, 60%) -->
      <div class="left-panel">
        <!-- Header: Title + Status -->
        <div class="detail-header">
          <h2 class="detail-title">{{ detailTitle }}</h2>
          <el-tag :type="statusTagType(detail.status)" size="large">{{ detail.statusLabel }}</el-tag>
        </div>

        <!-- Achievement Info (Read-only) -->
        <el-descriptions :column="1" border class="detail-descriptions">
          <!-- Paper Fields -->
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

          <!-- Patent Fields -->
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

          <!-- Copyright Fields -->
          <template v-if="achievementType === 'copyright'">
            <el-descriptions-item label="软著名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="著作权人">{{ detail.copyrightHolder }}</el-descriptions-item>
            <el-descriptions-item label="登记号">{{ detail.registrationNo }}</el-descriptions-item>
            <el-descriptions-item label="登记日期">{{ detail.registrationDate }}</el-descriptions-item>
            <el-descriptions-item label="版本号">{{ detail.softwareVersion }}</el-descriptions-item>
            <el-descriptions-item label="软件类别">{{ detail.softwareCategory }}</el-descriptions-item>
          </template>

          <!-- Common Fields -->
          <el-descriptions-item label="涉密标记">{{ detail.isClassified ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.isClassified" label="密级">{{ detail.classifiedLevel }}</el-descriptions-item>
          <el-descriptions-item label="所属课题">{{ detail.projectRef || '—' }}</el-descriptions-item>
          <el-descriptions-item label="归档号">{{ detail.archiveNo || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Attachments -->
        <div class="section">
          <h3 class="section-title">附件</h3>
          <el-empty v-if="attachments.length === 0" description="暂无附件" />
          <el-table v-else :data="attachments">
            <el-table-column prop="originalName" label="文件名" />
            <el-table-column prop="fileSizeDisplay" label="大小" />
            <el-table-column label="操作">
              <template #default="scope">
                <el-button size="small" @click="downloadAttachment(scope.row)">下载</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- Full Approval Timeline -->
        <div class="section">
          <h3 class="section-title">审批时间线</h3>
          <AchievementTimeline
            :records="approvalHistory"
            :show-pending="detail.status === 'PENDING_DEPT_REVIEW' || detail.status === 'PENDING_ADMIN_ARCHIVE'"
          />
        </div>
      </div>

      <!-- Right Panel (fixed, 40%) -->
      <div class="right-panel">
        <ApprovalActions
          :achievement-type="achievementType"
          :achievement-id="achievementId"
          :status="detail.status"
          :is-admin="isAdmin"
          :loading="actionLoading"
          @pass="handlePass"
          @reject="handleReject"
          @archive="handleArchive"
        />
        <ApprovalHistory :records="approvalHistory" :show-pending="isPending" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as paperApi from '@/api/achievement/paper'
import * as patentApi from '@/api/achievement/patent'
import * as copyrightApi from '@/api/achievement/copyright'
import * as approvalApi from '@/api/approval'
import * as attachmentApi from '@/api/attachment'
import AchievementTimeline from '@/components/achievement/AchievementTimeline.vue'
import ApprovalActions from '@/components/approval/ApprovalActions.vue'
import ApprovalHistory from '@/components/approval/ApprovalHistory.vue'
import { useUserStore } from '@/store/user'
import { useApprovalStore } from '@/stores/approval'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const approvalStore = useApprovalStore()

const loading = ref(false)
const actionLoading = ref(false)
const detail = ref<any>(null)
const achievementType = ref<string>('paper')
const achievementId = ref<number>(0)
const attachments = ref<any[]>([])
const approvalHistory = ref<any[]>([])

const isAdmin = computed(() => userStore.hasPermission('approval:admin') || userStore.roles.includes('ROLE_ADMIN'))
const isPending = computed(() =>
  detail.value?.status === 'PENDING_DEPT_REVIEW' || detail.value?.status === 'PENDING_ADMIN_ARCHIVE'
)

const detailTitle = computed(() => {
  if (!detail.value) return ''
  if (achievementType.value === 'paper') return detail.value.title
  if (achievementType.value === 'patent') return detail.value.patentName
  if (achievementType.value === 'copyright') return detail.value.name
  return ''
})

function detectType(data: any): string {
  if (data.title !== undefined) return 'paper'
  if (data.patentName !== undefined) return 'patent'
  if (data.name !== undefined) return 'copyright'
  return 'paper'
}

onMounted(async () => {
  const id = Number(route.params.id)
  const type = route.query.type as string
  if (!id) return

  achievementId.value = id
  loading.value = true

  try {
    let data: any = null
    const typesToTry = type ? [type] : ['paper', 'patent', 'copyright']

    for (const t of typesToTry) {
      try {
        const apiMap: Record<string, any> = {
          paper: paperApi.getById,
          patent: patentApi.getById,
          copyright: copyrightApi.getById,
        }
        const res: any = await apiMap[t](id)
        if (res?.data) {
          data = res.data
          achievementType.value = t
          break
        }
      } catch { /* try next type */ }
    }

    if (data) {
      detail.value = data
      // Load approval history
      const histRes: any = await approvalApi.getHistory(achievementType.value, id)
      if (histRes?.data) {
        approvalHistory.value = histRes.data
      }
    }

    // Load attachments
    try {
      const attRes: any = await attachmentApi.getAttachments(achievementType.value, id)
      if (attRes?.data) {
        attachments.value = attRes.data
      }
    } catch { /* attachments optional */ }
  } catch {
    ElMessage.error('加载审批详情失败')
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

function downloadAttachment(row: any) {
  if (row?.id) {
    attachmentApi.download(row.id)
  }
}

async function handlePass() {
  actionLoading.value = true
  try {
    const res: any = await approvalApi.approve(achievementType.value, achievementId.value)
    if (res?.code === 200) {
      ElMessage.success('审批通过')
      approvalStore.fetchPendingCount()
      router.push('/approval/pending')
    }
  } catch {
    ElMessage.error('审批操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleReject(reason: string) {
  actionLoading.value = true
  try {
    const res: any = await approvalApi.reject(achievementType.value, achievementId.value, reason)
    if (res?.code === 200) {
      ElMessage.warning('已退回至提交人')
      approvalStore.fetchPendingCount()
      router.push('/approval/pending')
    }
  } catch {
    ElMessage.error('退回操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleArchive(archiveNo: string) {
  actionLoading.value = true
  try {
    const res: any = await approvalApi.approve(achievementType.value, achievementId.value, '', archiveNo)
    if (res?.code === 200) {
      ElMessage.success(`已归档，编号：${archiveNo || '自动生成'}`)
      approvalStore.fetchPendingCount()
      router.push('/approval/pending')
    }
  } catch {
    ElMessage.error('归档操作失败')
  } finally {
    actionLoading.value = false
  }
}
</script>

<style scoped>
.approval-detail {
  width: 100%;
}

.split-layout {
  display: flex;
  gap: 16px;
  margin-top: 16px;
  min-height: calc(100vh - 200px);
}

.left-panel {
  flex: 3;
  overflow-y: auto;
  padding-right: 8px;
}

.right-panel {
  flex: 2;
  position: sticky;
  top: 16px;
  align-self: flex-start;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

/* Responsive: stack on narrow screens */
@media (max-width: 1365px) {
  .split-layout {
    flex-direction: column;
  }
  .right-panel {
    position: static;
    max-height: none;
  }
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0;
}

.detail-descriptions {
  margin-bottom: 24px;
}

.section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}
</style>
