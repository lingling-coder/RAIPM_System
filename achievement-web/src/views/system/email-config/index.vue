<template>
  <div class="email-config-page">
    <!-- Page Title -->
    <h2 class="page-title">邮件服务配置</h2>

    <!-- Error State -->
    <el-alert
      v-if="errorState"
      title="配置加载失败"
      type="error"
      show-icon
      :closable="false"
      class="mb-16"
    >
      <template #default>
        <el-button type="primary" link @click="fetchConfig">
          点击重试
        </el-button>
      </template>
    </el-alert>

    <!-- Empty State (no config) -->
    <div v-if="!hasConfig && !loading && !errorState" class="empty-state">
      <el-empty description="尚未配置邮件服务">
        <el-button type="primary" :icon="Plus" @click="showForm = true">
          配置邮件服务
        </el-button>
      </el-empty>
    </div>

    <!-- SMTP Config Form -->
    <div v-if="showForm" class="config-section">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="140px"
        label-position="right"
        class="smtp-form"
      >
        <div class="form-section-title">SMTP 服务器设置</div>

        <el-form-item label="SMTP服务器地址" prop="host" required>
          <el-input
            v-model="formData.host"
            placeholder="smtp.example.com"
            style="max-width: 360px"
          />
        </el-form-item>

        <el-form-item label="端口" prop="port" required>
          <el-input-number
            v-model="formData.port"
            :min="1"
            :max="65535"
            :step="1"
            style="width: 160px"
          />
          <span class="form-hint">默认 587 (STARTTLS) 或 465 (SSL)</span>
        </el-form-item>

        <el-form-item label="账号" prop="username">
          <el-input
            v-model="formData.username"
            placeholder="your-email@example.com"
            style="max-width: 360px"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            show-password
            :placeholder="isExistingConfig ? '留空则不修改' : '请输入SMTP密码'"
            style="max-width: 360px"
          />
        </el-form-item>

        <el-form-item label="发件人名称" prop="senderName">
          <el-input
            v-model="formData.senderName"
            placeholder="科研成果管理系统"
            style="max-width: 360px"
          />
        </el-form-item>

        <el-form-item label="启用TLS" prop="tls">
          <el-switch
            v-model="formData.tls"
            :active-value="true"
            :inactive-value="false"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">
            保存
          </el-button>
          <el-button v-if="!isExistingConfig" @click="handleCancel">
            取消
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- Test Email Section -->
    <div v-if="hasConfig" class="config-section">
      <el-divider />
      <h3 class="section-title">发送测试邮件</h3>

      <div class="test-email-row">
        <el-input
          v-model="testEmail"
          placeholder="输入测试接收邮箱"
          style="max-width: 360px; margin-right: 12px;"
        />
        <el-button
          type="primary"
          :loading="testLoading"
          :disabled="!testEmail"
          @click="handleTest"
        >
          发送测试邮件
        </el-button>
      </div>

      <!-- Test result -->
      <el-alert
        v-if="testResult"
        :title="testResult.success ? '连接成功' : '连接失败'"
        :type="testResult.success ? 'success' : 'error'"
        show-icon
        :closable="true"
        class="test-result"
        @close="testResult = null"
      >
        <template #default>
          <p>{{ testResult.message }}</p>
          <p v-if="testResult.success" class="response-time">
            响应时间: {{ testResult.responseTimeMs }}ms
          </p>
        </template>
      </el-alert>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getConfig, saveConfig, testConnection } from '@/api/reminder/email-config'
import type { EmailConfigDTO, TestResult } from '@/api/reminder/email-config'

// ── State ────────────────────────────────────────────────────────────────
const loading = ref(false)
const errorState = ref(false)
const showForm = ref(false)
const saving = ref(false)
const isExistingConfig = ref(false)
const formRef = ref<any>(null)

const defaultForm: EmailConfigDTO = {
  host: '',
  port: 587,
  username: '',
  password: '',
  senderName: '',
  tls: true,
}

const formData = reactive<EmailConfigDTO>({ ...defaultForm })

// Test email state
const testEmail = ref('')
const testLoading = ref(false)
const testResult = ref<TestResult | null>(null)

// ── Computed ─────────────────────────────────────────────────────────────
const hasConfig = computed(() => {
  return !!formData.host && formData.host.length > 0
})

// ── Form Validation Rules ────────────────────────────────────────────────
const formRules = {
  host: [
    { required: true, message: '请输入SMTP服务器地址', trigger: 'blur' },
  ],
  port: [
    { required: true, message: '请输入端口号', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入SMTP账号', trigger: 'blur' },
  ],
}

// ── Fetch Config ─────────────────────────────────────────────────────────
async function fetchConfig() {
  loading.value = true
  errorState.value = false
  try {
    const res = await getConfig()
    const data = res.data as EmailConfigDTO
    if (data && data.host) {
      Object.assign(formData, {
        host: data.host || '',
        port: data.port || 587,
        username: data.username || '',
        password: '',  // Never returned from backend
        senderName: data.senderName || '',
        tls: data.tls !== undefined ? data.tls : true,
      })
      isExistingConfig.value = true
      showForm.value = true
    } else {
      isExistingConfig.value = false
      showForm.value = false
    }
  } catch (err) {
    errorState.value = true
    console.error('Failed to fetch email config:', err)
  } finally {
    loading.value = false
  }
}

// ── Form Actions ─────────────────────────────────────────────────────────
function handleCancel() {
  showForm.value = false
  Object.assign(formData, { ...defaultForm })
  isExistingConfig.value = false
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    saving.value = true

    const payload: EmailConfigDTO = {
      host: formData.host,
      port: formData.port,
      username: formData.username,
      senderName: formData.senderName,
      tls: formData.tls,
    }

    // Only send password if user entered one (or if saving for the first time)
    if (formData.password || !isExistingConfig.value) {
      payload.password = formData.password || ''
    }

    await saveConfig(payload)
    ElMessage.success('配置保存成功')
    isExistingConfig.value = true
    showForm.value = true
    fetchConfig()  // Refresh to show saved state
  } catch (err: any) {
    if (err?.message) {
      ElMessage.error(err.message)
    }
  } finally {
    saving.value = false
  }
}

// ── Test Connection ──────────────────────────────────────────────────────
async function handleTest() {
  if (!testEmail.value) return
  testLoading.value = true
  testResult.value = null

  try {
    const res = await testConnection(testEmail.value)
    testResult.value = res.data as TestResult
  } catch (err: any) {
    testResult.value = {
      success: false,
      message: '请求失败: ' + (err?.message || '未知错误'),
      responseTimeMs: 0,
    }
  } finally {
    testLoading.value = false
  }
}

// ── Init ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchConfig()
})
</script>

<style scoped lang="scss">
.email-config-page {
  padding: 16px;

  .page-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    color: #303133;
  }

  .mb-16 {
    margin-bottom: 16px;
  }

  .empty-state {
    display: flex;
    justify-content: center;
    padding: 60px 0;
  }

  .config-section {
    max-width: 720px;

    .form-section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 20px;
      padding-bottom: 8px;
      border-bottom: 1px solid #ebeef5;
    }

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 16px;
    }
  }

  .smtp-form {
    max-width: 600px;
  }

  .form-hint {
    margin-left: 8px;
    color: #909399;
    font-size: 12px;
  }

  .test-email-row {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
  }

  .test-result {
    margin-top: 12px;

    .response-time {
      margin-top: 4px;
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>
