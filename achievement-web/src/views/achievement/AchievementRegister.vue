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

      <el-divider v-if="store.formData" />

      <!-- Common fields -->
      <template v-if="store.formData">
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
      </template>

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

    <!-- Duplicate detection dialog (D-46) -->
    <DuplicateDialog
      v-model:visible="duplicateDialogVisible"
      :duplicate-data="duplicateCheckData"
      :field-label="duplicateFieldLabel"
      @view-existing="handleViewExisting"
      @continue-submit="handleContinueSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAchievementStore } from '@/stores/achievement'
import * as paperApi from '@/api/achievement/paper'
import * as patentApi from '@/api/achievement/patent'
import * as copyrightApi from '@/api/achievement/copyright'
import * as invalidationApi from '@/api/achievement/invalidation'
import type { PaperFormDTO, DoiLookupResult } from '@/api/achievement/paper'
import type { DuplicateCheckResult } from '@/api/achievement/invalidation'
import PaperForm from '@/components/achievement/PaperForm.vue'
import PatentForm from '@/components/achievement/PatentForm.vue'
import CopyrightForm from '@/components/achievement/CopyrightForm.vue'
import DoiPreviewDialog from '@/components/achievement/DoiPreviewDialog.vue'
import AttachmentUploader from '@/components/achievement/AttachmentUploader.vue'
import DuplicateDialog from '@/components/achievement/DuplicateDialog.vue'

const store = useAchievementStore()
const router = useRouter()
const route = useRoute()

// ── State ──────────────────────────────────────────────────────────
const activeType = ref<'paper' | 'patent' | 'copyright'>('paper')
const formRef = ref<any>(null)
const submitting = ref(false)
const savingDraft = ref(false)
const isClassified = ref(false)

const doiDialogVisible = ref(false)
const doiPreviewData = ref<DoiLookupResult | null>(null)

// Duplicate check state (D-45~D-47)
const duplicateDialogVisible = ref(false)
const duplicateCheckData = ref<DuplicateCheckResult | null>(null)
const duplicateFieldLabel = ref('')
const pendingSubmit = ref(false) // Whether submit should proceed after duplicate dialog

// Initialize form data
onMounted(async () => {
  // CR-03: Read draftId from query params when navigating from edit action
  const draftId = route.query.draftId
  if (draftId) {
    const id = Number(draftId)
    if (id && !isNaN(id)) {
      // Determine type from query param or try all APIs via store
      const type = route.query.type as string | undefined
      if (type && ['paper', 'patent', 'copyright'].includes(type)) {
        store.switchType(type as 'paper' | 'patent' | 'copyright')
      }
      await store.loadDraft(id)
    }
  }
  if (!store.formData) {
    store.resetForm()
  }
})

// ── Dynamic Component ─────────────────────────────────────────────

const formComponentMap: Record<string, any> = {
  paper: PaperForm,
  patent: PatentForm,
  copyright: CopyrightForm,
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

async function onTypeChange(newType: any) {
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

function onFormUpdate(data: any) {
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
    title: data.title || (store.formData as any).title,
    authors: data.authors || (store.formData as any).authors,
    journal: data.journal || (store.formData as any).journal,
    volume: data.volume || (store.formData as any).volume,
    issue: data.issue || (store.formData as any).issue,
    pages: data.pages || (store.formData as any).pages,
    publishYear: data.publishYear || (store.formData as any).publishYear,
    abstractText: data.abstractText || (store.formData as any).abstractText,
  })
  ElMessage.success('DOI 数据已填入表单')
}

// ── Submit with Duplicate Check (D-45~D-47) ──────────────────────

/**
 * Get the unique identifying field for the current achievement type.
 * Return value type and field name (for duplicate check label display).
 */
function getUniqueField(): { value: string | null | undefined; label: string } {
  const form = store.formData as any
  if (!form) return { value: null, label: '' }

  switch (activeType.value) {
    case 'paper':
      return { value: form.doi, label: 'DOI' }
    case 'patent':
      return { value: form.applicationNo, label: '申请号' }
    case 'copyright':
      return { value: form.registrationNo, label: '登记号' }
    default:
      return { value: null, label: '' }
  }
}

/**
 * Check for duplicate before submitting.
 * D-45: Check at submit time.
 * D-47: Drafts skip duplicate check.
 */
async function checkDuplicateBeforeSubmit(): Promise<boolean> {
  const { value: uniqueField, label } = getUniqueField()

  // D-47: No unique field (or empty) means no check needed
  if (!uniqueField) {
    return true // No duplicate blocking, proceed
  }

  try {
    duplicateFieldLabel.value = label
    const res: any = await invalidationApi.checkDuplicate(activeType.value, uniqueField)
    if (res?.data?.duplicate) {
      // D-46: Show duplicate dialog
      duplicateCheckData.value = res.data
      duplicateDialogVisible.value = true
      return false // Block submit, wait for user decision
    }
  } catch {
    // If duplicate check fails, allow submit to proceed (degraded behavior)
    console.warn('Duplicate check failed, proceeding with submit')
  }

  return true
}

/**
 * Handle the actual form submission after all checks pass.
 */
async function doSubmit() {
  if (!formRef.value || !store.formData) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const apiMap: Record<string, any> = {
      paper: paperApi.submit,
      patent: patentApi.submit,
      copyright: copyrightApi.submit,
    }
    const submitApi = apiMap[activeType.value]
    if (!submitApi) {
      ElMessage.error('未知的成果类型')
      return
    }
    const res: any = await submitApi(store.formData)
    if (res?.code === 200 || res?.data) {
      ElMessage.success('提交成功！成果已进入审批流程')
      store.resetForm()
      isClassified.value = false
      // D-07: Stay on same type
    }
  } catch (err: any) {
    const msg = err?.response?.data?.message || err?.message || '提交失败，请稍后重试'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
    pendingSubmit.value = false
  }
}

/**
 * Submit click handler: validate, check duplicate, then submit.
 */
async function submitForm() {
  // Step 1: Validate form fields
  if (!formRef.value || !store.formData) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // Step 2: Check duplicate (D-45, D-47)
  const canProceed = await checkDuplicateBeforeSubmit()
  if (!canProceed) {
    // Duplicate dialog will be shown — user decides next step
    pendingSubmit.value = true
    return
  }

  // Step 3: Submit
  await doSubmit()
}

/**
 * D-46: User clicked [查看已有成果] — navigate to existing achievement detail.
 */
function handleViewExisting(id: number | undefined) {
  if (id) {
    duplicateDialogVisible.value = false
    router.push(`/achievement/detail/${id}`)
  }
}

/**
 * D-46: User clicked [继续填写并提交] — proceed with submission despite duplicate.
 */
async function handleContinueSubmit() {
  duplicateDialogVisible.value = false
  duplicateCheckData.value = null
  // Proceed with the actual submission
  await doSubmit()
}

// ── Save Draft ────────────────────────────────────────────────────

async function saveDraft() {
  const formData = store.formData as any
  const title = formData?.title || formData?.patentName || formData?.name
  if (!title) {
    ElMessage.warning('请输入标题/名称后再保存草稿')
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
