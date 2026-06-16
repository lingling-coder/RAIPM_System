<template>
  <div class="dict-management">
    <el-row :gutter="16" style="height: calc(100vh - 140px)">
      <!-- Left Tree Panel -->
      <el-col :span="6" class="left-panel">
        <el-card shadow="never" class="tree-card">
          <div class="tree-header">
            <h3>Dictionary Categories</h3>
            <el-button type="primary" size="small" :icon="Plus" @click="openCategoryDialog()">Add</el-button>
          </div>
          <el-tree
            ref="categoryTreeRef"
            :data="categoryTree"
            :props="{ label: 'categoryName' }"
            node-key="id"
            default-expand-all
            highlight-current
            :expand-on-click-node="false"
            @node-click="handleCategoryClick"
            :empty-text="'No categories'"
          >
            <template #default="{ node, data }">
              <span class="tree-node-label">{{ node.label }}</span>
              <span v-if="data.id" class="tree-node-actions">
                <el-button link type="primary" size="small" @click.stop="openCategoryDialog(data)">Edit</el-button>
                <el-button link type="danger" size="small" @click.stop="handleDeleteCategory(data)">Del</el-button>
              </span>
            </template>
          </el-tree>
        </el-card>
      </el-col>

      <!-- Right Entry Table -->
      <el-col :span="18" class="right-panel">
        <el-card shadow="never" class="table-card">
          <div class="toolbar">
            <div class="toolbar-left">
              <span class="current-category" v-if="selectedCategory">
                Entries: <strong>{{ selectedCategory.categoryName }}</strong>
              </span>
              <span class="current-category" v-else>
                <strong>All Dictionary Entries</strong>
              </span>
            </div>
            <div class="toolbar-right">
              <el-input
                v-model="entrySearch"
                placeholder="Search key/value..."
                clearable
                style="width: 220px"
                @keyup.enter="fetchEntries"
              />
              <el-button type="primary" :icon="Plus" @click="openEntryDialog()">Add Entry</el-button>
            </div>
          </div>

          <el-alert v-if="entryError" :title="entryError" type="error" show-icon closable class="error-alert" @close="entryError = ''" />

          <el-table
            v-loading="entryLoading"
            :data="entryList"
            stripe
            style="width: 100%"
            :empty-text="'No dictionary entries available'"
          >
            <el-table-column type="index" label="#" width="60" />
            <el-table-column prop="dictKey" label="Key" min-width="150">
              <template #default="{ row }">
                <code class="dict-key">{{ row.dictKey }}</code>
              </template>
            </el-table-column>
            <el-table-column prop="dictValue" label="Value" min-width="200" />
            <el-table-column prop="sortOrder" label="Sort" width="70" align="center" />
            <el-table-column prop="status" label="Status" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                  {{ row.status === 1 ? 'Active' : 'Disabled' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="Actions" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="openEntryDialog(row as any)">Edit</el-button>
                <el-button type="danger" link size="small" @click="handleDeleteEntry(row as any)">Delete</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="entryPagination.page"
              v-model:page-size="entryPagination.pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="entryPagination.total"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="fetchEntries"
              @current-change="fetchEntries"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Category Edit Dialog -->
    <el-dialog
      v-model="categoryDialogVisible"
      :title="categoryEditing ? 'Edit Category' : 'Add Category'"
      width="450px"
      @close="resetCategoryForm"
    >
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="100px">
        <el-form-item label="Category Name" prop="categoryName">
          <el-input v-model="categoryForm.categoryName" />
        </el-form-item>
        <el-form-item label="Category Code" prop="categoryCode">
          <el-input v-model="categoryForm.categoryCode" :disabled="categoryEditing" />
        </el-form-item>
        <el-form-item label="Sort Order">
          <el-input-number v-model="categoryForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="Description">
          <el-input v-model="categoryForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item v-if="categoryEditing" label="Status">
          <el-switch v-model="categoryForm.enabled" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="categorySubmitting" @click="submitCategory">
          {{ categoryEditing ? 'Save' : 'Create' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- Entry Edit Drawer -->
    <el-drawer
      v-model="entryDrawerVisible"
      :title="entryEditing ? 'Edit Entry' : 'Add Entry'"
      size="500px"
      @close="resetEntryForm"
    >
      <el-form ref="entryFormRef" :model="entryForm" :rules="entryRules" label-width="110px" label-position="top">
        <el-form-item label="Category" prop="categoryId">
          <el-select v-model="entryForm.categoryId" :disabled="entryEditing || !!selectedCategory" style="width: 100%">
            <el-option
              v-for="cat in filteredCategories"
              :key="cat.id!"
              :label="cat.categoryName"
              :value="cat.id!"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Key" prop="dictKey">
          <el-input v-model="entryForm.dictKey" placeholder="Unique key within category" />
        </el-form-item>
        <el-form-item label="Value" prop="dictValue">
          <el-input v-model="entryForm.dictValue" placeholder="Display value" />
        </el-form-item>
        <el-form-item label="Sort Order">
          <el-input-number v-model="entryForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item v-if="entryEditing" label="Status">
          <el-switch v-model="entryForm.enabled" active-value="1" inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="entryDrawerVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="entrySubmitting" @click="submitEntry">
          {{ entryEditing ? 'Save' : 'Create' }}
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as dictApi from '@/api/system/dict'
import type { DictCategoryData, DictEntryData } from '@/api/system/dict'

// ── Types ────────────────────────────────────────────────────────────────
interface CategoryNode extends DictCategoryData {
  [key: string]: any
}

// ── Categories ──────────────────────────────────────────────────────────
const categoryTree = ref<CategoryNode[]>([])
const categoryTreeRef = ref()
const selectedCategory = ref<CategoryNode | null>(null)

/** All categories without the root "All" node */
const filteredCategories = computed(() => {
  return categoryTree.value.filter(cat => cat.id != null)
})

const categoryDialogVisible = ref(false)
const categoryEditing = ref(false)
const categorySubmitting = ref(false)
const editingCategoryId = ref<number | null>(null)
const categoryFormRef = ref<FormInstance>()

const categoryForm = reactive({
  categoryName: '',
  categoryCode: '',
  description: '',
  sortOrder: 0,
  enabled: '1',
})

const categoryRules = {
  categoryName: [{ required: true, message: 'Category name is required', trigger: 'blur' }],
  categoryCode: [{ required: true, message: 'Category code is required', trigger: 'blur' }],
}

// ── Entry Data ───────────────────────────────────────────────────────────
const entryLoading = ref(false)
const entryError = ref('')
const entryList = ref<DictEntryData[]>([])
const entrySearch = ref('')

const entryPagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const entryDrawerVisible = ref(false)
const entryEditing = ref(false)
const entrySubmitting = ref(false)
const editingEntryId = ref<number | null>(null)
const entryFormRef = ref<FormInstance>()

const entryForm = reactive({
  categoryId: null as number | null,
  dictKey: '',
  dictValue: '',
  sortOrder: 0,
  enabled: '1',
})

const entryRules = {
  categoryId: [{ required: true, message: 'Category is required', trigger: 'change' }],
  dictKey: [{ required: true, message: 'Key is required', trigger: 'blur' }],
  dictValue: [{ required: true, message: 'Value is required', trigger: 'blur' }],
}

// ── API: Categories ──────────────────────────────────────────────────────
async function fetchCategories() {
  try {
    const res = await dictApi.listCategories()
    categoryTree.value = res.data || []
    // Add "All" root node
    categoryTree.value = [
      { id: null, categoryName: 'All Categories', categoryCode: 'ALL' } as any,
      ...categoryTree.value,
    ]
  } catch (e: any) {
    ElMessage.error(e.message || 'Failed to load categories')
  }
}

function handleCategoryClick(data: CategoryNode) {
  if (data.id === null) {
    selectedCategory.value = null
  } else {
    selectedCategory.value = data
  }
  entryPagination.page = 1
  fetchEntries()
}

// ── API: Entries ─────────────────────────────────────────────────────────
async function fetchEntries() {
  entryLoading.value = true
  entryError.value = ''
  try {
    const res = await dictApi.pageEntries({
      page: entryPagination.page,
      pageSize: entryPagination.pageSize,
      categoryId: selectedCategory.value?.id ?? undefined,
      keyword: entrySearch.value || undefined,
    })
    entryList.value = res.data.records || []
    entryPagination.total = res.data.total || 0
  } catch (e: any) {
    entryError.value = e.message || 'Failed to load entries'
  } finally {
    entryLoading.value = false
  }
}

// ── Category Dialog ──────────────────────────────────────────────────────
function openCategoryDialog(data?: CategoryNode) {
  if (data && data.id) {
    categoryEditing.value = true
    editingCategoryId.value = data.id
    categoryForm.categoryName = data.categoryName
    categoryForm.categoryCode = data.categoryCode
    categoryForm.description = data.description || ''
    categoryForm.sortOrder = data.sortOrder || 0
    categoryForm.enabled = String(data.status ?? 1)
  } else {
    categoryEditing.value = false
    editingCategoryId.value = null
  }
  categoryDialogVisible.value = true
}

function resetCategoryForm() {
  categoryForm.categoryName = ''
  categoryForm.categoryCode = ''
  categoryForm.description = ''
  categoryForm.sortOrder = 0
  categoryForm.enabled = '1'
  editingCategoryId.value = null
  categoryFormRef.value?.resetFields()
}

async function submitCategory() {
  const valid = await categoryFormRef.value?.validate().catch(() => false)
  if (!valid) return

  categorySubmitting.value = true
  try {
    const data: DictCategoryData = {
      categoryName: categoryForm.categoryName,
      categoryCode: categoryForm.categoryCode,
      description: categoryForm.description,
      sortOrder: categoryForm.sortOrder,
      status: Number(categoryForm.enabled),
    }
    if (categoryEditing.value && editingCategoryId.value) {
      await dictApi.updateCategory(editingCategoryId.value, data)
      ElMessage.success('Category updated')
    } else {
      await dictApi.createCategory(data)
      ElMessage.success('Category created')
    }
    categoryDialogVisible.value = false
    fetchCategories()
  } catch (e: any) {
    ElMessage.error(e.message || 'Operation failed')
  } finally {
    categorySubmitting.value = false
  }
}

async function handleDeleteCategory(data: CategoryNode) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete category "${data.categoryName}"?`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    if (!data.id) return
    await dictApi.removeCategory(data.id)
    ElMessage.success('Category deleted')
    if (selectedCategory.value?.id === data.id) {
      selectedCategory.value = null
    }
    fetchCategories()
    fetchEntries()
  } catch (e: any) {
    if (e.message) {
      ElMessage.error(e.message)
    }
  }
}

// ── Entry Drawer ─────────────────────────────────────────────────────────
function openEntryDialog(data?: DictEntryData) {
  if (data && data.id) {
    entryEditing.value = true
    editingEntryId.value = data.id
    entryForm.categoryId = data.categoryId
    entryForm.dictKey = data.dictKey
    entryForm.dictValue = data.dictValue
    entryForm.sortOrder = data.sortOrder || 0
    entryForm.enabled = String(data.status ?? 1)
  } else {
    entryEditing.value = false
    editingEntryId.value = null
    entryForm.categoryId = selectedCategory.value?.id ?? null
    entryForm.dictKey = ''
    entryForm.dictValue = ''
    entryForm.sortOrder = 0
    entryForm.enabled = '1'
  }
  entryDrawerVisible.value = true
}

function resetEntryForm() {
  entryForm.categoryId = selectedCategory.value?.id ?? null
  entryForm.dictKey = ''
  entryForm.dictValue = ''
  entryForm.sortOrder = 0
  entryForm.enabled = '1'
  editingEntryId.value = null
  entryFormRef.value?.resetFields()
}

async function submitEntry() {
  const valid = await entryFormRef.value?.validate().catch(() => false)
  if (!valid) return

  entrySubmitting.value = true
  try {
    const data: DictEntryData = {
      categoryId: entryForm.categoryId!,
      dictKey: entryForm.dictKey,
      dictValue: entryForm.dictValue,
      sortOrder: entryForm.sortOrder,
      status: Number(entryForm.enabled),
    }
    if (entryEditing.value && editingEntryId.value) {
      await dictApi.updateEntry(editingEntryId.value, data)
      ElMessage.success('Entry updated')
    } else {
      await dictApi.createEntry(data)
      ElMessage.success('Entry created')
    }
    entryDrawerVisible.value = false
    fetchEntries()
  } catch (e: any) {
    ElMessage.error(e.message || 'Operation failed')
  } finally {
    entrySubmitting.value = false
  }
}

async function handleDeleteEntry(data: any) {
  try {
    await ElMessageBox.confirm(
      `Are you sure you want to delete entry "${data.dictKey}"?`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    if (!data.id) return
    await dictApi.removeEntry(data.id)
    ElMessage.success('Entry deleted')
    fetchEntries()
  } catch (e: any) {
    if (e.message) {
      ElMessage.error(e.message)
    }
  }
}

// ── Init ─────────────────────────────────────────────────────────────────
onMounted(() => {
  fetchCategories()
  fetchEntries()
})
</script>

<style scoped lang="scss">
.dict-management {
  .left-panel {
    .tree-card {
      height: 100%;
      overflow-y: auto;
      .tree-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        h3 {
          margin: 0;
          font-size: 15px;
          font-weight: 600;
        }
      }
      .tree-node-label {
        font-size: 13px;
      }
      .tree-node-actions {
        float: right;
        font-size: 12px;
      }
    }
  }
  .right-panel {
    .table-card {
      height: 100%;
      display: flex;
      flex-direction: column;
      .toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        .toolbar-left {
          display: flex;
          align-items: center;
          .current-category {
            font-size: 14px;
            color: #606266;
          }
        }
        .toolbar-right {
          display: flex;
          gap: 8px;
        }
      }
      .error-alert {
        margin-bottom: 16px;
      }
      .dict-key {
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
}
</style>
