<template>
  <div class="department-management">
    <!-- Toolbar -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDrawer">Add Department</el-button>
      </div>

      <!-- Error State -->
      <el-alert v-if="errorState" title="Failed to load data" :description="errorState" type="error" show-icon closable class="error-alert" @close="errorState = ''" />

      <!-- Data Table -->
      <el-table
        v-loading="loading"
        :data="deptList"
        stripe
        style="width: 100%"
        :empty-text="'No department data available'"
      >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="deptName" label="Department Name" min-width="160" />
        <el-table-column prop="deptCode" label="Department Code" min-width="140">
          <template #default="{ row }">
            <code class="dept-code">{{ row.deptCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="leader" label="Leader" min-width="120" />
        <el-table-column prop="phone" label="Phone" min-width="140" />
        <el-table-column prop="memberCount" label="Members" width="80" align="center" />
        <el-table-column prop="status" label="Status" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDrawer(row as any)">Edit</el-button>
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
          @size-change="fetchDepartments"
          @current-change="fetchDepartments"
        />
      </div>
    </el-card>

    <!-- Create/Edit Drawer (flat structure, no parent dept per D-08) -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEditing ? 'Edit Department' : 'Add Department'"
      size="400px"
      @close="resetDrawerForm"
    >
      <el-form ref="drawerFormRef" :model="drawerForm" :rules="drawerRules" label-width="110px" label-position="top">
        <el-form-item label="Department Name" prop="deptName">
          <el-input v-model="drawerForm.deptName" />
        </el-form-item>
        <el-form-item label="Department Code" prop="deptCode">
          <el-input v-model="drawerForm.deptCode" :disabled="isEditing" />
        </el-form-item>
        <el-form-item label="Leader">
          <el-input v-model="drawerForm.leader" />
        </el-form-item>
        <el-form-item label="Phone">
          <el-input v-model="drawerForm.phone" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as deptApi from '@/api/system/department'
import type { DepartmentVO } from '@/api/system/department'

// ── Data ─────────────────────────────────────────────────────────────────
const loading = ref(false)
const errorState = ref('')
const deptList = ref<DepartmentVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// Drawer state
const drawerVisible = ref(false)
const drawerSubmitting = ref(false)
const isEditing = ref(false)
const editingDeptId = ref<number | null>(null)
const drawerFormRef = ref<FormInstance>()

const drawerForm = reactive({
  deptName: '',
  deptCode: '',
  leader: '',
  phone: '',
  enabled: '1',
})

const drawerRules = {
  deptName: [{ required: true, message: 'Department name is required', trigger: 'blur' }],
  deptCode: [{ required: true, message: 'Department code is required', trigger: 'blur' }],
}

// ── API Calls ────────────────────────────────────────────────────────────
async function fetchDepartments() {
  loading.value = true
  errorState.value = ''
  try {
    const res = await deptApi.page({ page: pagination.page, pageSize: pagination.pageSize })
    deptList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (e: any) {
    errorState.value = e.message || 'Failed to fetch department list'
  } finally {
    loading.value = false
  }
}

// ── Drawer ───────────────────────────────────────────────────────────────
function openCreateDrawer() {
  isEditing.value = false
  editingDeptId.value = null
  drawerVisible.value = true
}

function openEditDrawer(row: DepartmentVO) {
  isEditing.value = true
  editingDeptId.value = row.id
  drawerForm.deptName = row.deptName
  drawerForm.deptCode = row.deptCode
  drawerForm.leader = row.leader || ''
  drawerForm.phone = row.phone || ''
  drawerForm.enabled = String(row.status)
  drawerVisible.value = true
}

function resetDrawerForm() {
  drawerForm.deptName = ''
  drawerForm.deptCode = ''
  drawerForm.leader = ''
  drawerForm.phone = ''
  drawerForm.enabled = '1'
  editingDeptId.value = null
  drawerFormRef.value?.resetFields()
}

async function submitDrawer() {
  const valid = await drawerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  drawerSubmitting.value = true
  try {
    if (isEditing.value && editingDeptId.value) {
      await deptApi.update(editingDeptId.value, {
        deptName: drawerForm.deptName,
        deptCode: drawerForm.deptCode,
        leader: drawerForm.leader,
        phone: drawerForm.phone,
        status: Number(drawerForm.enabled),
      })
      ElMessage.success('Department updated')
    } else {
      await deptApi.create({
        deptName: drawerForm.deptName,
        deptCode: drawerForm.deptCode,
        leader: drawerForm.leader,
        phone: drawerForm.phone,
      })
      ElMessage.success('Department created')
    }
    drawerVisible.value = false
    fetchDepartments()
  } catch (e: any) {
    ElMessage.error(e.message || 'Operation failed')
  } finally {
    drawerSubmitting.value = false
  }
}

// ── Delete ───────────────────────────────────────────────────────────────
async function handleDelete(row: DepartmentVO) {
  try {
    await ElMessageBox.confirm(
      row.memberCount > 0
        ? `Cannot delete department "${row.deptName}": ${row.memberCount} member(s) are assigned. Remove all members first.`
        : `Are you sure you want to delete department "${row.deptName}"?`,
      'Confirm Delete',
      {
        confirmButtonText: row.memberCount > 0 ? 'OK' : 'Delete',
        cancelButtonText: 'Cancel',
        type: 'warning',
        confirmButtonClass: row.memberCount > 0 ? 'btn-disabled-warning' : '',
      }
    )
    if (row.memberCount > 0) return
    await deptApi.remove(row.id)
    ElMessage.success('Department deleted')
    fetchDepartments()
  } catch (e: any) {
    if (e.message) {
      ElMessage.error(e.message)
    }
  }
}

// ── Init ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchDepartments()
})
</script>

<style scoped lang="scss">
.department-management {
  .table-card {
    .toolbar {
      display: flex;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    .error-alert {
      margin-bottom: 16px;
    }
    .dept-code {
      background: #f5f7fa;
      padding: 2px 6px;
      border-radius: 3px;
      font-size: 12px;
      color: #606266;
    }
  }
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
    padding-top: 16px;
  }
}
</style>
