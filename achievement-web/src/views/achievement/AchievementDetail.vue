<template>
  <div class="achievement-detail" v-loading="loading">
    <el-breadcrumb>
      <el-breadcrumb-item :to="{ path: '/achievement/list' }">成果列表</el-breadcrumb-item>
      <el-breadcrumb-item>成果详情</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Header -->
    <div class="detail-header" v-if="paper">
      <div class="header-left">
        <span class="title-text">{{ paper.title }}</span>
        <el-tag v-if="paper.status" :type="statusTagType(paper.status)">{{ paper.statusLabel }}</el-tag>
        <el-tag v-if="paper.isClassified" type="warning" effect="dark">
          {{ paper.classifiedLevel || '涉密' }}
        </el-tag>
      </div>
    </div>

    <!-- Action bar -->
    <div class="action-bar" v-if="paper">
      <el-button v-if="paper.status === 'DRAFT'" type="primary" @click="editPaper">编辑</el-button>
    </div>

    <!-- Tabs -->
    <el-tabs v-model="activeTab" v-if="paper">
      <el-tab-pane label="基本信息" name="basic">
        <el-descriptions :column="1" border class="detail-descriptions">
          <el-descriptions-item label="论文标题">{{ paper.title }}</el-descriptions-item>
          <el-descriptions-item label="作者">{{ paper.authors }}</el-descriptions-item>
          <el-descriptions-item label="期刊/会议">{{ paper.journal }}</el-descriptions-item>
          <el-descriptions-item label="DOI">{{ paper.doi || '—' }}</el-descriptions-item>
          <el-descriptions-item label="ISSN/CN">{{ paper.issn || '—' }}</el-descriptions-item>
          <el-descriptions-item label="卷号">{{ paper.volume || '—' }}</el-descriptions-item>
          <el-descriptions-item label="期号">{{ paper.issue || '—' }}</el-descriptions-item>
          <el-descriptions-item label="页码">{{ paper.pages || '—' }}</el-descriptions-item>
          <el-descriptions-item label="发表年份">{{ paper.publishYear }}</el-descriptions-item>
          <el-descriptions-item label="收录情况">{{ paper.indexStatus }}</el-descriptions-item>
          <el-descriptions-item label="影响因子">{{ paper.impactFactor ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="中科院分区">{{ paper.zone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="摘要">{{ paper.abstractText || '—' }}</el-descriptions-item>
          <el-descriptions-item label="涉密标记">{{ paper.isClassified ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item v-if="paper.isClassified" label="密级">{{ paper.classifiedLevel }}</el-descriptions-item>
          <el-descriptions-item label="所属课题">{{ paper.projectRef || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ paper.statusLabel }}</el-descriptions-item>
          <el-descriptions-item label="归档号">{{ paper.archiveNo || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <el-tab-pane label="附件" name="attachments">
        <el-empty description="暂无附件" v-if="attachments.length === 0">
          <el-button type="primary">上传附件</el-button>
        </el-empty>
        <el-table v-else :data="attachments">
          <el-table-column prop="originalName" label="文件名" />
          <el-table-column prop="fileSizeDisplay" label="大小" />
          <el-table-column prop="uploadTime" label="上传时间" />
          <el-table-column label="操作">
            <template #default="scope">
              <el-button size="small" @click="downloadAttachment(scope.row)">下载</el-button>
              <el-button v-if="paper?.status === 'DRAFT' || paper?.status === 'REJECTED'"
                size="small" type="danger" @click="deleteAttachment(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { PaperVO } from '@/api/achievement/paper'
import * as paperApi from '@/api/achievement/paper'
import * as attachmentApi from '@/api/attachment'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const paper = ref<PaperVO | null>(null)
const activeTab = ref('basic')
const attachments = ref<any[]>([])

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res: any = await paperApi.getById(id)
    if (res?.data) {
      paper.value = res.data
    }
    // Load attachments
    try {
      const attRes: any = await attachmentApi.getAttachments('paper', id)
      if (attRes?.data) {
        attachments.value = attRes.data
      }
    } catch {
      // Attachments may not be available
    }
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

function editPaper() {
  router.push(`/achievement/register?draftId=${paper.value?.id}`)
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
  } catch {
    // Cancelled
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

.title-text {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.action-bar {
  margin-bottom: 16px;
}

.detail-descriptions {
  margin-top: 16px;
}
</style>
