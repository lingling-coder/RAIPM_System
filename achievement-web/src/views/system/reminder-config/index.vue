<template>
  <div class="reminder-config-page">
    <!-- Page Title -->
    <h2 class="page-title">提醒配置管理</h2>

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
    >
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="typeName" label="提醒类型" width="120" />
      <el-table-column prop="titleTemplate" label="提醒标题" width="200" show-overflow-tooltip />
      <el-table-column label="紧急等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag
            :type="urgencyTagType((row as ReminderConfigVO).urgency)"
            size="small"
            effect="dark"
          >
            {{ urgencyLabel((row as ReminderConfigVO).urgency) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提前天数" width="90" align="center">
        <template #default="{ row }">
          {{ (row as ReminderConfigVO).advanceDays }}天
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag
            :type="(row as ReminderConfigVO).status === 1 ? 'success' : 'info'"
            size="small"
            effect="plain"
          >
            {{ (row as ReminderConfigVO).status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleEdit(row as ReminderConfigVO)">
            编辑
          </el-button>
          <el-button type="danger" link size="small" @click="handleDelete(row as ReminderConfigVO)">
            删除
          </el-button>
        </template>
      </el-table-column>

      <!-- Empty State -->
      <template #empty>
        <el-empty
          image="empty"
          description="暂无提醒配置，点击新增配置添加提醒规则"
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
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- ── Config Editor Drawer ────────────────────────────────────────────── -->
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
          <!-- Tab 1: 基础设置 -->
          <el-tab-pane label="基础设置" name="basic">
            <el-form-item label="提醒类型" prop="typeCode" required>
              <el-select
                v-model="formData.typeCode"
                placeholder="选择提醒类型"
                style="width: 100%"
                :disabled="!!editingId"
              >
                <el-option
                  v-for="type in reminderTypeOptions"
                  :key="type.code"
                  :label="type.label"
                  :value="type.code"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="提醒标题" prop="titleTemplate" required>
              <el-input
                v-model="formData.titleTemplate"
                placeholder="例如：项目申报提醒 — {achievementName}"
              />
            </el-form-item>
            <el-form-item label="紧急等级" prop="urgency" required>
              <el-select v-model="formData.urgency" placeholder="选择紧急等级" style="width: 100%">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
            <el-form-item label="提前天数" prop="advanceDays">
              <el-input-number
                v-model="formData.advanceDays"
                :min="1"
                :max="365"
                :step="1"
              />
              <span class="form-suffix">天</span>
            </el-form-item>
            <el-form-item label="调度规则" prop="schedulingRule">
              <el-input
                v-model="formData.schedulingRule"
                placeholder="cron表达式（可选）"
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

          <!-- Tab 2: 接收人 -->
          <el-tab-pane label="接收人" name="receiver">
            <el-form-item label="负责人" prop="responsibleUserId">
              <el-select
                v-model="formData.responsibleUserId"
                placeholder="选择负责人"
                style="width: 100%"
                clearable
                filterable
              >
                <el-option
                  v-for="user in userOptions"
                  :key="user.id"
                  :label="user.realName || user.username"
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="负责角色" prop="responsibleRoleCode">
              <el-select
                v-model="formData.responsibleRoleCode"
                placeholder="选择负责角色"
                style="width: 100%"
                clearable
              >
                <el-option label="科研秘书" value="ROLE_SECRETARY" />
                <el-option label="部门管理员" value="ROLE_DEPT_ADMIN" />
                <el-option label="院领导" value="ROLE_LEADER" />
              </el-select>
            </el-form-item>
          </el-tab-pane>

          <!-- Tab 3: 模板内容 -->
          <el-tab-pane label="模板内容" name="template">
            <el-form-item label="成果名称" prop="achievementName">
              <el-input
                v-model="formData.achievementName"
                placeholder="例如：高性能芯片设计"
              />
            </el-form-item>
            <el-form-item label="正文模板" prop="bodyTemplate">
              <el-input
                v-model="formData.bodyTemplate"
                type="textarea"
                :rows="8"
                placeholder="请输入提醒正文模板"
              />
              <div class="variable-hints">
                <span class="variable-hint-title">可用变量：</span>
                <el-tag
                  v-for="variable in templateVariables"
                  :key="variable"
                  size="small"
                  type="info"
                  effect="plain"
                  class="variable-tag"
                >
                  {{ variable }}
                </el-tag>
              </div>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { page, getById, create, update, remove } from '@/api/reminder/reminder-config'
import type { ReminderConfigDTO, ReminderConfigVO } from '@/api/reminder/reminder-config'

// ── Reminder Type Enum Options ───────────────────────────────────────────────
const reminderTypeOptions = [
  { code: 'PROJECT_APPLICATION', label: '项目申报' },
  { code: 'AWARD_APPLICATION', label: '奖项申报' },
  { code: 'PATENT_ANNUAL_FEE', label: '专利年费' },
  { code: 'COPYRIGHT_MAINTENANCE', label: '软著维护' },
  { code: 'TRANSFORMATION_EVAL', label: '转化后评估' },
  { code: 'CLASSIFIED_AUDIT', label: '涉密成果定期核查' },
]

// ── Template Variables ───────────────────────────────────────────────────────
const templateVariables = ['{achievementName}', '{deadline}', '{daysRemaining}', '{responsiblePerson}']

// ── State ────────────────────────────────────────────────────────────────────
const tableData = ref<ReminderConfigVO[]>([])
const tableLoading = ref(false)
const errorState = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// Drawer state
const drawerVisible = ref(false)
const drawerTitle = ref('新增提醒配置')
const activeTab = ref('basic')
const saving = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<any>(null)

const defaultForm: ReminderConfigDTO = {
  typeCode: '',
  achievementName: '',
  titleTemplate: '',
  bodyTemplate: '',
  urgency: 'MEDIUM',
  advanceDays: 30,
  schedulingRule: '',
  responsibleUserId: undefined,
  responsibleRoleCode: '',
  status: 1,
}

const formData = reactive<ReminderConfigDTO>({ ...defaultForm })

// User options (placeholder — would be fetched from user API in production)
const userOptions = ref<Array<{ id: number; username: string; realName?: string }>>([])

// ── Form Validation Rules ────────────────────────────────────────────────────
const formRules = {
  typeCode: [{ required: true, message: '请选择提醒类型', trigger: 'change' }],
  titleTemplate: [{ required: true, message: '请输入提醒标题', trigger: 'blur' }],
  urgency: [{ required: true, message: '请选择紧急等级', trigger: 'change' }],
}

// ── Urgency Helpers ──────────────────────────────────────────────────────────
function urgencyTagType(urgency: string): string {
  const map: Record<string, string> = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'primary',
  }
  return map[urgency] || 'info'
}

function urgencyLabel(urgency: string): string {
  const map: Record<string, string> = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低',
  }
  return map[urgency] || urgency
}

// ── Fetch Data ───────────────────────────────────────────────────────────────
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
    console.error('Failed to fetch reminder configs:', err)
  } finally {
    tableLoading.value = false
  }
}

// ── Pagination Handlers ──────────────────────────────────────────────────────
function handleSizeChange(val: number) {
  pagination.pageSize = val
  pagination.page = 1
  fetchData()
}

function handleCurrentChange(val: number) {
  pagination.page = val
  fetchData()
}

// ── Drawer Actions ───────────────────────────────────────────────────────────
function handleAdd() {
  drawerTitle.value = '新增提醒配置'
  editingId.value = null
  activeTab.value = 'basic'
  Object.assign(formData, { ...defaultForm })
  drawerVisible.value = true
}

async function handleEdit(row: ReminderConfigVO) {
  drawerTitle.value = '编辑提醒配置'
  editingId.value = row.id
  activeTab.value = 'basic'
  try {
    const res = await getById(row.id)
    const data = res.data
    Object.assign(formData, {
      typeCode: data.typeCode,
      achievementName: data.achievementName,
      titleTemplate: data.titleTemplate,
      bodyTemplate: data.bodyTemplate || '',
      urgency: data.urgency,
      advanceDays: data.advanceDays,
      schedulingRule: data.schedulingRule || '',
      responsibleUserId: data.responsibleUserId || undefined,
      responsibleRoleCode: data.responsibleRoleCode || '',
      status: data.status,
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

    if (editingId.value) {
      await update(editingId.value, formData)
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

// ── Delete ───────────────────────────────────────────────────────────────────
async function handleDelete(row: ReminderConfigVO) {
  try {
    await ElMessageBox.confirm(
      `确定要删除提醒配置"${row.typeName}"吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        draggable: true,
      }
    )
    await remove(row.id)
    ElMessage.success('配置已删除')
    fetchData()
  } catch {
    // cancelled
  }
}

// ── Init ─────────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.reminder-config-page {
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
}

.config-form {
  padding: 0 8px;

  .form-suffix {
    margin-left: 8px;
    color: #909399;
    font-size: 14px;
  }

  .variable-hints {
    margin-top: 8px;
    display: flex;
    align-items: center;
    gap: 4px;
    flex-wrap: wrap;

    .variable-hint-title {
      color: #909399;
      font-size: 12px;
      margin-right: 4px;
    }

    .variable-tag {
      cursor: default;
    }
  }
}
</style>
