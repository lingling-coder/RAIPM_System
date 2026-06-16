<template>
  <div class="api-config-page">
    <!-- Page Title -->
    <h2 class="page-title">API集成配置</h2>

    <!-- Error State -->
    <el-alert
      v-if="errorState"
      title="数据加载失败"
      type="error"
      show-icon
      :closable="false"
      class="mb-16"
    >
      <template #default>
        <el-button type="primary" link @click="fetchData">
          点击重试
        </el-button>
      </template>
    </el-alert>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-button type="primary" :icon="Plus" @click="handleAdd">
        新增配置
      </el-button>
    </div>

    <!-- Table -->
    <el-table
      v-loading="tableLoading"
      :data="tableData"
      stripe
      border
      style="width: 100%"
      @sort-change="handleSortChange"
    >
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="configName" label="配置名称" width="180" show-overflow-tooltip />
      <el-table-column prop="endpointUrl" label="接口地址" width="250" show-overflow-tooltip />
      <el-table-column label="超时时间" width="120" align="center">
        <template #default="{ row }">
          {{ (row as any).connectTimeout || 5 }}s / {{ (row as any).readTimeout || 10 }}s
        </template>
      </el-table-column>
      <el-table-column label="重试次数" width="100" align="center">
        <template #default="{ row }">
          {{ (row as any).retryCount || 3 }}次
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="(row as any).status === 1 ? 'success' : 'info'" size="small" effect="plain">
            {{ (row as any).status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最后测试" width="160" align="center">
        <template #default="{ row }">
          <template v-if="(row as any).lastTestTime">
            <span>{{ formatDateTime((row as any).lastTestTime) }}</span>
            <el-tag
              v-if="(row as any).lastTestResult !== null && (row as any).lastTestResult !== undefined"
              :type="(row as any).lastTestResult === 1 ? 'success' : 'danger'"
              size="small"
              effect="plain"
              class="ml-4"
            >
              {{ (row as any).lastTestResult === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
          <span v-else class="text-secondary">未测试</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleTest(row as any)">
            测试连接
          </el-button>
          <el-button type="primary" link size="small" @click="handleEdit(row as any)">
            编辑
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row as any)">
            删除
          </el-button>
        </template>
      </el-table-column>

      <!-- Empty State -->
      <template #empty>
        <el-empty
          image="empty"
          description="暂无API配置，点击新增配置添加外部接口"
        >
          <el-button type="primary" :icon="Plus" @click="handleAdd">
            新增配置
          </el-button>
        </el-empty>
      </template>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- ── Config Editor Drawer ──────────────────────────────────────────── -->
    <el-drawer
      v-model="drawerVisible"
      :title="drawerTitle"
      direction="rtl"
      size="600px"
      :close-on-click-modal="true"
      @close="handleDrawerClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        label-position="right"
        class="config-form"
      >
        <el-tabs v-model="activeTab">
          <!-- Tab 1: 基础配置 -->
          <el-tab-pane label="基础配置" name="basic">
            <el-form-item label="配置名称" prop="configName" required>
              <el-input v-model="formData.configName" placeholder="例如：Crossref API" />
            </el-form-item>
            <el-form-item label="配置编码" prop="configCode" required>
              <el-input
                v-model="formData.configCode"
                placeholder="例如：crossref_api"
                :disabled="!!formData.id"
              />
            </el-form-item>
            <el-form-item label="接口地址" prop="endpointUrl" required>
              <el-input
                v-model="formData.endpointUrl"
                placeholder="https://api.crossref.org"
                type="url"
              />
            </el-form-item>
            <el-form-item label="描述" prop="description">
              <el-input
                v-model="formData.description"
                type="textarea"
                :rows="3"
                placeholder="接口用途说明"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-switch
                v-model="formData.status"
                :active-value="1"
                :inactive-value="0"
                active-text="启用"
                inactive-text="停用"
              />
            </el-form-item>
          </el-tab-pane>

          <!-- Tab 2: 认证配置 -->
          <el-tab-pane label="认证配置" name="auth">
            <el-form-item label="认证方式" prop="authType">
              <el-select v-model="formData.authType" placeholder="选择认证方式" style="width: 100%">
                <el-option label="None" value="NONE" />
                <el-option label="API Key" value="API_KEY" />
                <el-option label="Bearer Token" value="BEARER" />
                <el-option label="Basic Auth" value="BASIC" />
              </el-select>
            </el-form-item>
            <el-form-item label="API Key" prop="apiKey">
              <el-input
                v-model="formData.apiKey"
                :placeholder="formData.id ? '留空则不修改' : '请输入API密钥'"
                show-password
              />
            </el-form-item>
            <el-form-item
              v-if="isBasicAuth"
              label="Secret Key"
              prop="secretKey"
            >
              <el-input
                v-model="formData.secretKey"
                :placeholder="formData.id ? '留空则不修改' : '请输入秘密密钥'"
                show-password
              />
            </el-form-item>
            <el-form-item label="Token URL" prop="tokenUrl">
              <el-input
                v-model="formData.tokenUrl"
                placeholder="OAuth2 Token URL（可选）"
              />
            </el-form-item>
          </el-tab-pane>

          <!-- Tab 3: 高级设置 -->
          <el-tab-pane label="高级设置" name="advanced">
            <el-form-item label="连接超时" prop="connectTimeout">
              <el-input-number
                v-model="formData.connectTimeout"
                :min="1"
                :max="30"
                :step="1"
              />
              <span class="form-suffix">秒</span>
            </el-form-item>
            <el-form-item label="读取超时" prop="readTimeout">
              <el-input-number
                v-model="formData.readTimeout"
                :min="1"
                :max="60"
                :step="1"
              />
              <span class="form-suffix">秒</span>
            </el-form-item>
            <el-form-item label="重试次数" prop="retryCount">
              <el-input-number
                v-model="formData.retryCount"
                :min="0"
                :max="5"
                :step="1"
              />
              <span class="form-suffix">次</span>
            </el-form-item>
            <el-form-item label="重试间隔" prop="retryInterval">
              <el-input-number
                v-model="formData.retryInterval"
                :min="1"
                :max="10"
                :step="1"
              />
              <span class="form-suffix">秒</span>
            </el-form-item>
            <el-form-item label="退避策略" prop="backoffStrategy">
              <el-select v-model="formData.backoffStrategy" style="width: 200px">
                <el-option label="指数退避" value="EXPONENTIAL" />
                <el-option label="固定间隔" value="FIXED" />
              </el-select>
            </el-form-item>
            <el-form-item label="失败告警" prop="failureAlert">
              <el-switch
                v-model="formData.failureAlert"
                :active-value="1"
                :inactive-value="0"
                active-text="开启"
                inactive-text="关闭"
              />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>

      <!-- Drawer Footer -->
      <template #footer>
        <el-button @click="handleDrawerClose">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-drawer>

    <!-- ── Test Connection Dialog ────────────────────────────────────────── -->
    <el-dialog
      v-model="testDialogVisible"
      :title="'测试连接 - ' + (testingConfig?.configName || '')"
      width="440px"
      :close-on-click-modal="false"
    >
      <div class="test-connection-body">
        <!-- Testing in progress -->
        <div v-if="testLoading" class="test-progress">
          <el-progress :percentage="100" :stroke-width="6" striped striped-flow />
          <p class="test-status-text">正在测试连接...</p>
        </div>

        <!-- Test result: success -->
        <el-result
          v-if="testResult && testResult.success"
          icon="success"
          title="连接成功"
          :sub-title="'响应时间: ' + testResult.responseTimeMs + 'ms'"
        >
          <template #extra>
            <el-button type="primary" @click="testDialogVisible = false">
              关闭
            </el-button>
          </template>
        </el-result>

        <!-- Test result: failure -->
        <el-result
          v-if="testResult && !testResult.success"
          icon="error"
          title="连接失败"
          :sub-title="testResult.message"
        >
          <template #extra>
            <el-button type="primary" @click="testDialogVisible = false">
              关闭
            </el-button>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { page, getById, create, update, remove, testConnection } from '@/api/system/api-config'
import type { ApiConfigDTO, TestConnectionResultVO } from '@/api/system/api-config'

// ── State ────────────────────────────────────────────────────────────────
const tableData = ref<ApiConfigDTO[]>([])
const tableLoading = ref(false)
const errorState = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// Drawer state
const drawerVisible = ref(false)
const drawerTitle = ref('新增API配置')
const activeTab = ref('basic')
const saving = ref(false)
const formRef = ref<any>(null)

const defaultForm: ApiConfigDTO = {
  configName: '',
  configCode: '',
  endpointUrl: '',
  description: '',
  authType: 'NONE',
  apiKey: '',
  secretKey: '',
  tokenUrl: '',
  connectTimeout: 5,
  readTimeout: 10,
  retryCount: 3,
  retryInterval: 1,
  backoffStrategy: 'EXPONENTIAL',
  failureAlert: 0,
  status: 1,
}

const formData = reactive<ApiConfigDTO>({ ...defaultForm })

// Test connection state
const testDialogVisible = ref(false)
const testLoading = ref(false)
const testResult = ref<TestConnectionResultVO | null>(null)
const testingConfig = ref<ApiConfigDTO | null>(null)

// ── Computed ─────────────────────────────────────────────────────────────
const isBasicAuth = computed(() => formData.authType === 'BASIC')

// ── Form Validation Rules ────────────────────────────────────────────────
const formRules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  configCode: [{ required: true, message: '请输入配置编码', trigger: 'blur' }],
  endpointUrl: [
    { required: true, message: '请输入接口地址', trigger: 'blur' },
    { type: 'url' as any, message: '请输入有效的URL地址', trigger: 'blur' },
  ],
}

// ── Fetch Data ───────────────────────────────────────────────────────────
async function fetchData() {
  tableLoading.value = true
  errorState.value = false
  try {
    const res = await page({
      page: pagination.page,
      pageSize: pagination.pageSize,
    })
    tableData.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (err) {
    errorState.value = true
    console.error('Failed to fetch API configs:', err)
  } finally {
    tableLoading.value = false
  }
}

// ── Pagination Handlers ──────────────────────────────────────────────────
function handleSizeChange(val: number) {
  pagination.pageSize = val
  pagination.page = 1
  fetchData()
}

function handleCurrentChange(val: number) {
  pagination.page = val
  fetchData()
}

// ── Sort Handler ─────────────────────────────────────────────────────────
function handleSortChange() {
  // Placeholder for future sort functionality
}

// ── Drawer Actions ───────────────────────────────────────────────────────
function handleAdd() {
  drawerTitle.value = '新增API配置'
  activeTab.value = 'basic'
  Object.assign(formData, { ...defaultForm })
  drawerVisible.value = true
}

async function handleEdit(row: ApiConfigDTO) {
  drawerTitle.value = '编辑API配置'
  activeTab.value = 'basic'
  try {
    const res = await getById(row.id!)
    const data = res.data
    Object.assign(formData, {
      ...data,
      // Keep existing API key/secret if masked (user is not changing them)
      apiKey: data.apiKey && data.apiKey !== '****' ? data.apiKey : '',
      secretKey: data.secretKey && data.secretKey !== '****' ? data.secretKey : '',
    })
    drawerVisible.value = true
  } catch (err) {
    ElMessage.error('获取配置详情失败')
  }
}

function handleDrawerClose() {
  drawerVisible.value = false
  formRef.value?.resetFields()
}

async function handleSave() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    saving.value = true

    if (formData.id) {
      await update(formData.id, formData)
      ElMessage.success('配置更新成功')
    } else {
      await create(formData)
      ElMessage.success('配置创建成功')
    }

    drawerVisible.value = false
    fetchData()
  } catch (err: any) {
    if (err?.message) {
      ElMessage.error(err.message)
    }
  } finally {
    saving.value = false
  }
}

// ── Test Connection ──────────────────────────────────────────────────────
async function handleTest(row: ApiConfigDTO) {
  testingConfig.value = row
  testResult.value = null
  testLoading.value = true
  testDialogVisible.value = true

  try {
    const res = await testConnection(row.id!)
    testResult.value = res.data
  } catch (err: any) {
    testResult.value = {
      success: false,
      message: '请求失败: ' + (err?.message || '未知错误'),
      responseTimeMs: 0,
      statusCode: null,
    }
  } finally {
    testLoading.value = false
  }
}

// ── Delete ───────────────────────────────────────────────────────────────
async function handleDelete(row: ApiConfigDTO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除API配置"${row.configName}"吗？`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        draggable: true,
      }
    )
    await remove(row.id!)
    ElMessage.success('配置已删除')
    fetchData()
  } catch {
    // cancelled
  }
}

// ── Helpers ──────────────────────────────────────────────────────────────
function formatDateTime(dateStr: string): string {
  if (!dateStr) return ''
  // Format: YYYY-MM-DD HH:mm
  return dateStr.substring(0, 16).replace('T', ' ')
}

// ── Init ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.api-config-page {
  padding: 16px;

  .page-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    color: #303133;
  }

  .toolbar {
    margin-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .pagination-wrapper {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .mb-16 {
    margin-bottom: 16px;
  }

  .ml-4 {
    margin-left: 4px;
  }

  .text-secondary {
    color: #909399;
    font-size: 12px;
  }
}

.config-form {
  padding: 0 8px;

  .form-suffix {
    margin-left: 8px;
    color: #909399;
    font-size: 14px;
  }
}

.test-connection-body {
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;

  .test-progress {
    width: 100%;
    text-align: center;

    .test-status-text {
      margin-top: 16px;
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>
