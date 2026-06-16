<template>
  <div class="achievement-register">
    <h2 class="page-title">成果登记</h2>

    <!-- Type selector -->
    <el-radio-group v-model="activeType" @change="onTypeChange" class="type-selector">
      <el-radio-button value="paper">论文</el-radio-button>
      <el-radio-button value="patent">专利</el-radio-button>
      <el-radio-button value="copyright">软件著作权</el-radio-button>
    </el-radio-group>

    <!-- Draft recovery alert -->
    <el-alert
      v-if="store.currentDraftId"
      title="已恢复草稿"
      type="info"
      show-icon
      :closable="false"
      class="draft-alert"
    />

    <el-form
      ref="formRef"
      :model="store.formData || undefined"
      :rules="formRules"
      label-width="120px"
      class="register-form"
    >
      <!-- Dynamic form per type -->
      <component
        :is="currentFormComponent"
        :key="activeType"
        v-if="store.formData"
        :model-value="store.formData"
        @update:model-value="onFormUpdate"
        @doi-ready="onDoiReady"
      />

      <el-divider />

      <!-- Common fields -->
      <el-form-item label="涉密标记">
        <el-switch v-model="isClassified" />
        <el-select
          v-if="isClassified"
          v-model="store.formData.classifiedLevel"
          placeholder="请选择密级"
          style="width: 150px; margin-left: 12px"
        >
          <el-option label="秘密" value="秘密" />
          <el-option label="机密" value="机密" />
        </el-select>
      </el-form-item>

      <el-form-item label="所属课题">
        <el-input v-model="store.formData.projectRef" placeholder="请输入课题名称/编号（自由文本）" />
      </el-form-item>

      <!-- Attachment upload -->
      <el-form-item label="附件">
        <AttachmentUploader
          :achievement-type="activeType"
          :achievement-id="store.currentDraftId || undefined"
        />
      </el-form-item>

      <!-- Action buttons -->
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submitForm">提交</el-button>
        <el-button :loading="savingDraft" @click="saveDraft">保存草稿</el-button>
      </el-form-item>
    </el-form>

    <!-- DOI preview dialog -->
    <DoiPreviewDialog
      v-model:visible="doiDialogVisible"
      :data="doiPreviewData"
      @confirm="applyDoiData"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAchievementStore } from '@/stores/achievement'
import * as paperApi from '@/api/achievement/paper'
import type { PaperFormDTO, DoiLookupResult } from '@/api/achievement/paper'
import PaperForm from '@/components/achievement/PaperForm.vue'
import DoiPreviewDialog from '@/components/achievement/DoiPreviewDialog.vue'
import AttachmentUploader from '@/components/achievement/AttachmentUploader.vue'

const store = useAchievementStore()

// ── State ──────────────────────────────────────────────────────────
const activeType = ref<'paper' | 'patent' | 'copyright'>('paper')
const formRef = ref<any>(null)
const submitting = ref(false)
const savingDraft = ref(false)
const isClassified = ref(false)

const doiDialogVisible = ref(false)
const doiPreviewData = ref<DoiLookupResult | null>(null)

// Initialize form data
onMounted(() => {
  if (!store.formData) {
    store.resetForm()
  }
})

// ── Dynamic Component ─────────────────────────────────────────────

const formComponentMap: Record<string, any> = {
  paper: PaperForm,
  // patent and copyright forms added in later plans
}

const currentFormComponent = computed(() => formComponentMap[activeType.value])

// ── Validation Rules ──────────────────────────────────────────────

const formRules = {
  title: [
    { required: true, message: '请输入论文标题', trigger: 'blur' },
    { max: 500, message: '标题不超过500字', trigger: 'blur' },
  ],
}

// ── Type Switch (D-01) ────────────────────────────────────────────

async function onTypeChange(newType: string) {
  if (store.currentDraftId) {
    try {
      await ElMessageBox.confirm(
        '切换成果类型将清空当前填写内容，是否继续？',
        '切换确认',
        { confirmButtonText: '继续切换', cancelButtonText: '取消', type: 'warning' }
      )
    } catch {
      // Revert the radio selection
      activeType.value = store.currentType
      return
    }
  }
  store.switchType(newType as 'paper' | 'patent' | 'copyright')
}

// ── Form Update ───────────────────────────────────────────────────

function onFormUpdate(data: PaperFormDTO) {
  store.formData = data
}

// ── DOI Integration ───────────────────────────────────────────────

function onDoiReady(data: DoiLookupResult) {
  doiPreviewData.value = data
  doiDialogVisible.value = true
}

function applyDoiData() {
  if (!doiPreviewData.value || !store.formData) return
  const data = doiPreviewData.value
  store.setFormData({
    title: data.title || store.formData.title,
    authors: data.authors || store.formData.authors,
    journal: data.journal || store.formData.journal,
    volume: data.volume || store.formData.volume,
    issue: data.issue || store.formData.issue,
    pages: data.pages || store.formData.pages,
    publishYear: data.publishYear || store.formData.publishYear,
    abstractText: data.abstractText || store.formData.abstractText,
  })
  ElMessage.success('DOI 数据已填入表单')
}

// ── Submit (D-07: stay on same page) ──────────────────────────────

async function submitForm() {
  if (!formRef.value || !store.formData) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res: any = await paperApi.submit(store.formData)
    if (res?.code === 200 || res?.data) {
      ElMessage.success('提交成功！成果已进入审批流程')
      store.resetForm()
      isClassified.value = false
      // D-07: Stay on same type
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// ── Save Draft ────────────────────────────────────────────────────

async function saveDraft() {
  if (!store.formData?.title) {
    ElMessage.warning('请输入标题后再保存草稿')
    return
  }

  savingDraft.value = true
  try {
    await store.saveDraft()
    ElMessage.success('草稿已保存')
  } catch {
    ElMessage.error('草稿保存失败，请重试')
  } finally {
    savingDraft.value = false
  }
}
</script>

<style scoped>
.achievement-register {
  max-width: 960px;
  margin: 0 auto;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 20px;
  color: #303133;
}

.type-selector {
  margin-bottom: 20px;
}

.draft-alert {
  margin-bottom: 16px;
}

.register-form {
  margin-top: 16px;
}
</style>
