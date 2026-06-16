<template>
  <div class="role-management">
    <!-- Toolbar -->
    <el-card shadow="never" class="table-card">
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openCreateDrawer">Add Role</el-button>
      </div>

      <!-- Error State -->
      <el-alert v-if="errorState" title="Failed to load data" :description="errorState" type="error" show-icon closable class="error-alert" @close="errorState = ''" />

      <!-- Data Table -->
      <el-table
        v-loading="loading"
        :data="roleList"
        stripe
        style="width: 100%"
        :empty-text="'No role data available'"
      >
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="roleName" label="Role Name" min-width="140" />
        <el-table-column prop="roleCode" label="Role Code" min-width="150">
          <template #default="{ row }">
            <code class="role-code">{{ row.roleCode }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="Description" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userCount" label="Users" width="80" align="center" />
        <el-table-column prop="status" label="Status" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? 'Active' : 'Disabled' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="Created At" width="170">
          <template #default="{ row }">
            {{ row.createdAt || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openPermissionDrawer(row as any)">Permissions</el-button>
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
          @size-change="fetchRoles"
          @current-change="fetchRoles"
        />
      </div>
    </el-card>

    <!-- Permission Assignment Drawer -->
    <el-drawer
      v-model="permDrawerVisible"
      :title="'Edit Permissions - ' + currentRoleName"
      size="600px"
      @close="resetPermDrawer"
    >
      <div class="perm-drawer-body">
        <el-input
          v-model="permSearch"
          placeholder="Search menu items..."
          clearable
          class="perm-search"
        />
        <el-tree
          v-loading="treeLoading"
          ref="menuTreeRef"
          :data="menuTreeData"
          :props="{ label: 'label', children: 'children' }"
          show-checkbox
          node-key="id"
          default-expand-all
          :filter-node-method="filterTreeNode"
          class="perm-tree"
        />
      </div>
      <template #footer>
        <el-button @click="permDrawerVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="permSubmitting" @click="submitPermission">Save Permissions</el-button>
      </template>
    </el-drawer>

    <!-- Create/Edit Drawer -->
    <el-drawer
      v-model="drawerVisible"
      :title="isEditing ? 'Edit Role' : 'Add Role'"
      size="400px"
      @close="resetDrawerForm"
    >
      <el-form ref="drawerFormRef" :model="drawerForm" :rules="drawerRules" label-width="100px" label-position="top">
        <el-form-item label="Role Name" prop="roleName">
          <el-input v-model="drawerForm.roleName" />
        </el-form-item>
        <el-form-item label="Role Code" prop="roleCode">
          <el-input v-model="drawerForm.roleCode" :disabled="isEditing" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="drawerForm.description" type="textarea" :rows="3" />
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
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as roleApi from '@/api/system/role'
import type { RoleVO, MenuTreeNode } from '@/api/system/role'

// ── Data ─────────────────────────────────────────────────────────────────
const loading = ref(false)
const errorState = ref('')
const roleList = ref<RoleVO[]>([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

// Permission drawer
const permDrawerVisible = ref(false)
const permSubmitting = ref(false)
const treeLoading = ref(false)
const currentRoleId = ref<number | null>(null)
const currentRoleName = ref('')
const menuTreeData = ref<MenuTreeNode[]>([])
const menuTreeRef = ref()
const permSearch = ref('')

// Create/Edit drawer
const drawerVisible = ref(false)
const drawerSubmitting = ref(false)
const isEditing = ref(false)
const editingRoleId = ref<number | null>(null)
const drawerFormRef = ref<FormInstance>()

const drawerForm = reactive({
  roleName: '',
  roleCode: '',
  description: '',
  enabled: '1',
})

const drawerRules = {
  roleName: [{ required: true, message: 'Role name is required', trigger: 'blur' }],
  roleCode: [{ required: true, message: 'Role code is required', trigger: 'blur' }],
}

// ── API Calls ────────────────────────────────────────────────────────────
async function fetchRoles() {
  loading.value = true
  errorState.value = ''
  try {
    const res = await roleApi.page({ page: pagination.page, pageSize: pagination.pageSize })
    roleList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (e: any) {
    errorState.value = e.message || 'Failed to fetch role list'
  } finally {
    loading.value = false
  }
}

// ── Permission Drawer ────────────────────────────────────────────────────
async function openPermissionDrawer(row: RoleVO) {
  currentRoleId.value = row.id
  currentRoleName.value = row.roleName
  permDrawerVisible.value = true
  treeLoading.value = true
  try {
    const res = await roleApi.getMenuTree(row.id)
    menuTreeData.value = res.data || []
    // Set checked nodes after tree renders
    setTimeout(() => {
      setCheckedNodes(res.data || [])
    }, 100)
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to load menu tree')
  } finally {
    treeLoading.value = false
  }
}

function setCheckedNodes(nodes: MenuTreeNode[]) {
  const checkedIds: number[] = []
  function collect(nodes: MenuTreeNode[]) {
    for (const node of nodes) {
      if (node.checked) {
        checkedIds.push(node.id)
      }
      if (node.children) {
        collect(node.children)
      }
    }
  }
  collect(nodes)
  if (checkedIds.length > 0 && menuTreeRef.value) {
    menuTreeRef.value.setCheckedKeys(checkedIds)
  }
}

function resetPermDrawer() {
  currentRoleId.value = null
  currentRoleName.value = ''
  menuTreeData.value = []
  permSearch.value = ''
}

async function submitPermission() {
  if (!currentRoleId.value) return
  permSubmitting.value = true
  try {
    const checkedKeys = menuTreeRef.value?.getCheckedKeys(false) || []
    const halfCheckedKeys = menuTreeRef.value?.getHalfCheckedKeys() || []
    const allKeys = [...checkedKeys, ...halfCheckedKeys]
    await roleApi.assignMenuPermissions(currentRoleId.value, allKeys)
    ElMessage.success('Permissions saved')
    permDrawerVisible.value = false
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to save permissions')
  } finally {
    permSubmitting.value = false
  }
}

// Tree search filter
watch(permSearch, (val) => {
  menuTreeRef.value?.filter(val)
})

function filterTreeNode(value: string, data: any): boolean {
  if (!value) return true
  return (data.label || '').toLowerCase().includes(value.toLowerCase())
}

// ── Create/Edit Drawer ───────────────────────────────────────────────────
function openCreateDrawer() {
  isEditing.value = false
  editingRoleId.value = null
  drawerVisible.value = true
}

function openEditDrawer(row: RoleVO) {
  isEditing.value = true
  editingRoleId.value = row.id
  drawerForm.roleName = row.roleName
  drawerForm.roleCode = row.roleCode
  drawerForm.description = row.description || ''
  drawerForm.enabled = String(row.status)
  drawerVisible.value = true
}

function resetDrawerForm() {
  drawerForm.roleName = ''
  drawerForm.roleCode = ''
  drawerForm.description = ''
  drawerForm.enabled = '1'
  editingRoleId.value = null
  drawerFormRef.value?.resetFields()
}

async function submitDrawer() {
  const valid = await drawerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  drawerSubmitting.value = true
  try {
    if (isEditing.value && editingRoleId.value) {
      await roleApi.update(editingRoleId.value, {
        roleName: drawerForm.roleName,
        description: drawerForm.description,
        status: Number(drawerForm.enabled),
      })
      ElMessage.success('Role updated')
    } else {
      await roleApi.create({
        roleName: drawerForm.roleName,
        roleCode: drawerForm.roleCode,
        description: drawerForm.description,
      })
      ElMessage.success('Role created')
    }
    drawerVisible.value = false
    fetchRoles()
  } catch (e: any) {
    ElMessage.error(e.message || 'Operation failed')
  } finally {
    drawerSubmitting.value = false
  }
}

// ── Delete ───────────────────────────────────────────────────────────────
async function handleDelete(row: RoleVO) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete role "${row.roleName}"?`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await roleApi.remove(row.id)
    ElMessage.success('Role deleted')
    fetchRoles()
  } catch (e: any) {
    if (e.message) {
      ElMessage.error(e.message)
    }
  }
}

// ── Init ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchRoles()
})
</script>

<style scoped lang="scss">
.role-management {
  .table-card {
    .toolbar {
      display: flex;
      justify-content: space-between;
      margin-bottom: 16px;
    }
    .error-alert {
      margin-bottom: 16px;
    }
    .role-code {
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
  .perm-drawer-body {
    .perm-search {
      margin-bottom: 16px;
    }
    .perm-tree {
      max-height: calc(100vh - 250px);
      overflow-y: auto;
    }
  }
}
</style>
