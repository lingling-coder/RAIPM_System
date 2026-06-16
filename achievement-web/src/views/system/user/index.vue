<template>
  <div class="user-management">
    <!-- Search Form -->
    <el-card shadow="never" class="search-card">
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="Keyword">
          <el-input v-model="searchForm.keyword" placeholder="Username / Name" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="Department">
          <el-select v-model="searchForm.deptId" placeholder="All Departments" clearable style="width: 180px">
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Role">
          <el-select v-model="searchForm.roleId" placeholder="All Roles" clearable style="width: 180px">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Status">
          <el-select v-model="searchForm.status" placeholder="All" clearable style="width: 120px">
            <el-option label="Active" :value="1" />
            <el-option label="Disabled" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">Search</el-button>
          <el-button @click="handleReset">Reset</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Toolbar -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-button type="primary" :icon="Plus" @click="openCreateDrawer">Add User</el-button>
          <el-button :icon="Upload" @click="openImportDialog">Import</el-button>
          <el-button :icon="Download" @click="handleExport">Export</el-button>
          <el-button type="danger" :icon="Delete" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
            Batch Delete ({{ selectedIds.length }})
          </el-button>
        </div>
      </div>

      <!-- Loading + Error State -->
      <el-alert v-if="errorState" title="Failed to load data" :description="errorState" type="error" show-icon closable class="error-alert" @close="errorState = ''" />

      <!-- Data Table -->
      <el-table
        v-loading="loading"
        :data="userList"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :empty-text="'No user data available'"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="username" label="Username" min-width="120" />
        <el-table-column prop="realName" label="Name" min-width="100" />
        <el-table-column prop="deptName" label="Department" min-width="120" />
        <el-table-column label="Roles" min-width="180">
          <template #default="{ row }">
            <el-tag v-for="role in row.roleNames || []" :key="role" size="small" class="role-tag">{{ role }}</el-tag>
            <span v-if="!row.roleNames || row.roleNames.length === 0" class="no-data">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="Phone" width="140">
          <template #default="{ row }">
            {{ maskPhone(row.phone) }}
          </template>
        </el-table-column>
        <el-table-column prop="email" label="Email" min-width="180" />
        <el-table-column prop="status" label="Status" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="Last Login" width="170">
          <template #default="{ row }">
            {{ row.lastLoginTime ? row.lastLoginTime : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDrawer(row as any)">Edit</el-button>
            <el-button type="warning" link size="small" @click="handleResetPassword(row as any)">Reset Pwd</el-button>
            <el-button
              :type="(row as any).status === 1 ? 'warning' : 'success'"
              link
              size="small"
              @click="handleToggleStatus(row as any)"
            >
              {{ (row as any).status === 1 ? 'Disable' : 'Enable' }}
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row as any)">Delete</el-button>
          </template>
        </el-table-column>
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
          @size-change="fetchUsers"
          @current-change="fetchUsers"
        />
      </div>
    </el-card>

    <!-- Create/Edit Drawer -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEditing ? 'Edit User' : 'Add User'"
      size="500px"
      @close="resetDrawerForm"
    >
      <el-form ref="drawerFormRef" :model="drawerForm" :rules="drawerRules" label-width="100px" label-position="top">
        <el-form-item label="Username" prop="username">
          <el-input v-model="drawerForm.username" :disabled="isEditing" />
        </el-form-item>
        <el-form-item v-if="!isEditing" label="Password" prop="password">
          <el-input v-model="drawerForm.password" type="password" show-password />
          <div class="form-help">Min 8 characters, must contain letters and numbers</div>
        </el-form-item>
        <el-form-item label="Name">
          <el-input v-model="drawerForm.realName" />
        </el-form-item>
        <el-form-item label="Department">
          <el-select v-model="drawerForm.deptId" placeholder="Select department" clearable style="width: 100%">
            <el-option v-for="dept in deptOptions" :key="dept.id" :label="dept.deptName" :value="dept.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Roles">
          <el-select v-model="drawerForm.roleIds" multiple placeholder="Select roles" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="drawerForm.phone" />
        </el-form-item>
        <el-form-item label="Email" prop="email">
          <el-input v-model="drawerForm.email" />
        </el-form-item>
        <el-form-item v-if="isEditing" label="Status">
          <el-switch v-model="drawerForm.enabled" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawerVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="drawerSubmitting" @click="submitDrawer">
          {{ isEditing ? 'Save' : 'Create' }}
        </el-button>
      </template>
    </el-drawer>

    <!-- Import Dialog -->
    <el-dialog v-model="importDialogVisible" title="Import Users from CSV" width="600px" :close-on-click-modal="false">
      <div class="import-body">
        <div class="import-info">
          <span>Upload a CSV file with columns: username, realName, email, phone, deptCode</span>
          <el-button link type="primary" size="small" @click="downloadTemplate">Download Template</el-button>
        </div>
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          accept=".csv"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="() => ElMessage.warning('Only one file allowed')"
        >
          <el-icon class="el-icon--upload" :size="40"><UploadFilled /></el-icon>
          <div class="el-upload__text">Drop CSV file here or <em>click to browse</em></div>
          <template #tip>
            <div class="el-upload__tip">.csv files only, max 1000 rows</div>
          </template>
        </el-upload>
        <div v-if="importResult" class="import-result">
          <el-divider />
          <h4>Import Result</h4>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="result-stat success">{{ importResult.inserted }} Inserted</div>
            </el-col>
            <el-col :span="8">
              <div class="result-stat warning">{{ importResult.updated }} Updated</div>
            </el-col>
            <el-col :span="8">
              <div class="result-stat danger">{{ importResult.failed }} Failed</div>
            </el-col>
          </el-row>
          <el-alert v-if="importResult.errors && importResult.errors.length > 0" title="Errors" type="error" show-icon :closable="false" class="import-errors">
            <template #default>
              <div v-for="(err, idx) in importResult.errors.slice(0, 5)" :key="idx" class="error-line">{{ err }}</div>
              <div v-if="importResult.errors.length > 5" class="error-line">... and {{ importResult.errors.length - 5 }} more</div>
            </template>
          </el-alert>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeImportDialog">Close</el-button>
        <el-button type="primary" :loading="importSubmitting" :disabled="!selectedFile" @click="submitImport">
          Import
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus, Upload, Download, Delete, UploadFilled } from '@element-plus/icons-vue'
import * as userApi from '@/api/system/user'
import * as roleApi from '@/api/system/role'
import * as deptApi from '@/api/system/department'
import type { UserVO, UserCreateData, UserUpdateData } from '@/api/system/user'
import type { RoleVO } from '@/api/system/role'
import type { DepartmentVO } from '@/api/system/department'

// ── Data ─────────────────────────────────────────────────────────────────
const loading = ref(false)
const errorState = ref('')
const userList = ref<UserVO[]>([])
const selectedIds = ref<number[]>([])
const deptOptions = ref<DepartmentVO[]>([])
const roleOptions = ref<RoleVO[]>([])
const selectedFile = ref<File | null>(null)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const searchForm = reactive({
  keyword: '',
  deptId: null as number | null,
  roleId: null as number | null,
  status: null as number | null,
})

// Drawer state
const drawerVisible = ref(false)
const drawerSubmitting = ref(false)
const isEditing = ref(false)
const editingUserId = ref<number | null>(null)
const drawerFormRef = ref<FormInstance>()

const drawerForm = reactive({
  username: '',
  password: '',
  realName: '',
  deptId: null as number | null,
  roleIds: [] as number[],
  phone: '',
  email: '',
  enabled: '1',
})

const drawerRules = {
  username: [{ required: true, message: 'Username is required', trigger: 'blur' }],
  password: [
    { required: true, message: 'Password is required', trigger: 'blur' },
    { min: 8, message: 'Password must be at least 8 characters', trigger: 'blur' },
  ],
  email: [{ type: 'email', message: 'Invalid email format', trigger: 'blur' }],
}

// Import state
const importDialogVisible = ref(false)
const importSubmitting = ref(false)
const importResult = ref<userApi.ImportResult | null>(null)
const uploadRef = ref()

// ── API Calls ────────────────────────────────────────────────────────────
async function fetchUsers() {
  loading.value = true
  errorState.value = ''
  try {
    const res = await userApi.page({
      page: pagination.page,
      pageSize: pagination.pageSize,
      keyword: searchForm.keyword || undefined,
      deptId: searchForm.deptId || undefined,
      roleId: searchForm.roleId || undefined,
      status: searchForm.status ?? undefined,
    })
    userList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (e: any) {
    errorState.value = e.message || 'Failed to fetch user list'
  } finally {
    loading.value = false
  }
}

async function fetchDropdowns() {
  try {
    const [deptRes, roleRes] = await Promise.all([
      deptApi.listAll(),
      roleApi.listAll(),
    ])
    deptOptions.value = deptRes.data || []
    roleOptions.value = roleRes.data || []
  } catch {
    // Silently fail - dropdowns will just be empty
  }
}

// ── Search ───────────────────────────────────────────────────────────────
function handleSearch() {
  pagination.page = 1
  fetchUsers()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.deptId = null
  searchForm.roleId = null
  searchForm.status = null
  pagination.page = 1
  fetchUsers()
}

// ── Selection ────────────────────────────────────────────────────────────
function handleSelectionChange(rows: UserVO[]) {
  selectedIds.value = rows.map((r) => r.id)
}

// ── Drawer ───────────────────────────────────────────────────────────────
function openCreateDrawer() {
  isEditing.value = false
  editingUserId.value = null
  drawerVisible.value = true
}

function openEditDrawer(row: UserVO) {
  isEditing.value = true
  editingUserId.value = row.id
  drawerForm.username = row.username
  drawerForm.realName = row.realName
  drawerForm.deptId = row.deptId
  drawerForm.roleIds = row.roleIds || []
  drawerForm.phone = row.phone || ''
  drawerForm.email = row.email || ''
  drawerForm.enabled = String(row.status)
  drawerForm.password = ''
  drawerVisible.value = true
}

function resetDrawerForm() {
  drawerForm.username = ''
  drawerForm.password = ''
  drawerForm.realName = ''
  drawerForm.deptId = null
  drawerForm.roleIds = []
  drawerForm.phone = ''
  drawerForm.email = ''
  drawerForm.enabled = '1'
  editingUserId.value = null
  drawerFormRef.value?.resetFields()
}

async function submitDrawer() {
  const valid = await drawerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  drawerSubmitting.value = true
  try {
    if (isEditing.value && editingUserId.value) {
      const data: UserUpdateData = {
        realName: drawerForm.realName,
        email: drawerForm.email,
        phone: drawerForm.phone,
        deptId: drawerForm.deptId,
        roleIds: drawerForm.roleIds,
        status: Number(drawerForm.enabled),
      }
      await userApi.update(editingUserId.value, data)
      ElMessage.success('User updated successfully')
    } else {
      const data: UserCreateData = {
        username: drawerForm.username,
        password: drawerForm.password,
        realName: drawerForm.realName,
        email: drawerForm.email,
        phone: drawerForm.phone,
        deptId: drawerForm.deptId,
        roleIds: drawerForm.roleIds,
      }
      await userApi.create(data)
      ElMessage.success('User created successfully')
    }
    drawerVisible.value = false
    fetchUsers()
  } catch (e: any) {
    ElMessage.error(e.message || 'Operation failed')
  } finally {
    drawerSubmitting.value = false
  }
}

// ── Actions ──────────────────────────────────────────────────────────────
async function handleDelete(row: UserVO) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete user "${row.username}"? The user account will be disabled but data will be preserved.`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await userApi.remove(row.id)
    ElMessage.success('User deleted')
    fetchUsers()
  } catch {
    // User cancelled
  }
}

async function handleBatchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete ${selectedIds.value.length} user(s)?`,
      'Confirm Batch Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await userApi.batchDelete(selectedIds.value)
    ElMessage.success(`${selectedIds.value.length} user(s) deleted`)
    selectedIds.value = []
    fetchUsers()
  } catch {
    // User cancelled
  }
}

async function handleToggleStatus(row: UserVO) {
  const newStatus = row.status === 1 ? 0 : 1
  const actionText = newStatus === 0 ? 'disable' : 'enable'
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to ${actionText} user "${row.username}"?`,
      `Confirm ${actionText === 'disable' ? 'Disable' : 'Enable'}`,
      { confirmButtonText: 'Confirm', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await userApi.setStatus(row.id, newStatus)
    ElMessage.success(`User ${actionText}d successfully`)
    fetchUsers()
  } catch {
    // User cancelled
  }
}

async function handleResetPassword(row: UserVO) {
  try {
    await ElMessageBox.confirm(
      `Reset password for user "${row.username}" to default?`,
      'Confirm Reset Password',
      { confirmButtonText: 'Reset', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await userApi.resetPassword(row.id)
    ElMessage.success('Password reset to default')
  } catch {
    // User cancelled
  }
}

// ── Import ───────────────────────────────────────────────────────────────
function openImportDialog() {
  importDialogVisible.value = true
  importResult.value = null
  selectedFile.value = null
}

function closeImportDialog() {
  importDialogVisible.value = false
  importResult.value = null
  selectedFile.value = null
}

function handleFileChange(file: any) {
  selectedFile.value = file.raw
}

async function submitImport() {
  if (!selectedFile.value) return
  importSubmitting.value = true
  try {
    const res = await userApi.importCsv(selectedFile.value)
    importResult.value = res.data
    if (res.data.failed === 0) {
      ElMessage.success(`Import complete: ${res.data.inserted} inserted, ${res.data.updated} updated`)
    }
    fetchUsers()
  } catch (e: any) {
    ElMessage.error(e.message || 'Import failed')
  } finally {
    importSubmitting.value = false
  }
}

function downloadTemplate() {
  const csvContent = '﻿username,realName,email,phone,deptCode\nzhangsan,Zhang San,zhangsan@institute.cn,13800138000,R001\nlisi,Li Si,lisi@institute.cn,13900139000,R002'
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = 'user_import_template.csv'
  link.click()
  URL.revokeObjectURL(link.href)
}

// ── Export ───────────────────────────────────────────────────────────────
function handleExport() {
  ElMessage.info('Export feature coming soon')
}

// ── Helpers ──────────────────────────────────────────────────────────────
function maskPhone(phone: string | null | undefined): string {
  if (!phone || phone.length < 7) return phone || '-'
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

// ── Init ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchUsers()
  fetchDropdowns()
})
</script>

<style scoped lang="scss">
.user-management {
  .search-card {
    margin-bottom: 16px;
    .search-form {
      display: flex;
      flex-wrap: wrap;
    }
  }
  .table-card {
    .toolbar {
      display: flex;
      justify-content: space-between;
      margin-bottom: 16px;
      .toolbar-left {
        display: flex;
        gap: 8px;
      }
    }
    .error-alert {
      margin-bottom: 16px;
    }
    .role-tag {
      margin-right: 4px;
      margin-bottom: 2px;
    }
    .no-data {
      color: #909399;
    }
  }
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
    padding-top: 16px;
  }
  .form-help {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
  .import-body {
    .import-info {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      font-size: 14px;
      color: #606266;
    }
    .import-result {
      margin-top: 16px;
      .result-stat {
        text-align: center;
        padding: 12px;
        border-radius: 4px;
        font-weight: bold;
        font-size: 16px;
        &.success { background: #f0f9eb; color: #67c23a; }
        &.warning { background: #fdf6ec; color: #e6a23c; }
        &.danger { background: #fef0f0; color: #f56c6c; }
      }
      .import-errors {
        margin-top: 12px;
        .error-line {
          font-size: 12px;
          line-height: 1.6;
        }
      }
    }
  }
}
</style>
