<template>
  <div class="paper-form">
    <el-form-item label="论文标题" prop="title" :rules="titleRules">
      <div class="title-search-row">
        <el-input v-model="form.title" placeholder="请输入论文标题" maxlength="500" show-word-limit class="title-input" />
        <el-button
          type="primary"
          plain
          :loading="searchLoading"
          :disabled="!form.title"
          @click="triggerSearch"
          class="smart-match-btn"
        >
          <el-icon><Search /></el-icon>
          智能匹配
        </el-button>
      </div>
      <div class="smart-match-tip">
        <el-icon><InfoFilled /></el-icon>
        <span>智能匹配会搜索 CrossRef 和 OpenAlex 学术数据库。建议同时填写标题和<strong>至少一位作者</strong>以获得更精确的结果。中文知网论文请将 DOI 粘贴到下方 DOI 字段自动补全。</span>
      </div>
    </el-form-item>

    <el-form-item label="作者" prop="authors" :rules="authorsRules">
      <el-input v-model="form.authors" placeholder="多个作者请用分号分隔" />
    </el-form-item>

    <el-form-item label="期刊/会议名称" prop="journal" :rules="journalRules">
      <el-input v-model="form.journal" placeholder="请输入期刊或会议名称" />
    </el-form-item>

    <el-form-item label="DOI" prop="doi">
      <DoiAutoComplete v-model="form.doi" @doi-result="onDoiResult" />
    </el-form-item>

    <el-form-item label="ISSN/CN" prop="issn">
      <el-input v-model="form.issn" placeholder="如有请填写" />
    </el-form-item>

    <el-row :gutter="16">
      <el-col :span="8">
        <el-form-item label="卷号" prop="volume">
          <el-input-number v-model="form.volume" :min="1" :max="9999" placeholder="卷号" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="期号" prop="issue">
          <el-input-number v-model="form.issue" :min="1" :max="9999" placeholder="期号" />
        </el-form-item>
      </el-col>
      <el-col :span="8">
        <el-form-item label="页码" prop="pages">
          <el-input v-model="form.pages" placeholder="如: 123-130" />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="发表年份" prop="publishYear" :rules="publishYearRules">
      <el-date-picker
        v-model="form.publishYear"
        type="year"
        placeholder="请选择发表年份"
        value-format="YYYY"
        style="width: 200px"
      />
    </el-form-item>

    <el-form-item label="收录情况" prop="indexStatus" :rules="indexStatusRules">
      <el-select v-model="form.indexStatus" placeholder="请选择收录情况" style="width: 300px">
        <el-option v-for="opt in indexStatusOptions" :key="opt" :label="opt" :value="opt" />
      </el-select>
    </el-form-item>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="影响因子" prop="impactFactor">
          <el-input-number
            v-model="form.impactFactor"
            :precision="3"
            :min="0"
            :max="999.999"
            placeholder="请输入影响因子"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="中科院分区" prop="zone">
          <el-select v-model="form.zone" placeholder="请选择分区" style="width: 100%">
            <el-option v-for="opt in zoneOptions" :key="opt" :label="opt" :value="opt" />
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="摘要" prop="abstractText">
      <el-input
        v-model="form.abstractText"
        type="textarea"
        :rows="4"
        placeholder="请输入论文摘要"
        maxlength="2000"
        show-word-limit
      />
    </el-form-item>

    <!-- Intelligent matching dialog -->
    <LitSearchDialog
      v-model:visible="searchDialogVisible"
      :results="searchResults"
      :loading="searchLoading"
      @confirm="onSearchResultConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, InfoFilled } from '@element-plus/icons-vue'
import type { PaperFormDTO, DoiLookupResult, LiteratureSearchItem } from '@/api/achievement/paper'
import { searchLiterature } from '@/api/achievement/paper'
import DoiAutoComplete from './DoiAutoComplete.vue'
import LitSearchDialog from './LitSearchDialog.vue'

const props = defineProps<{
  modelValue: PaperFormDTO
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: PaperFormDTO): void
  (e: 'doi-ready', data: DoiLookupResult): void
}>()

const form: any = reactive({ ...props.modelValue })

watch(
  () => ({ ...form }),
  (val) => {
    emit('update:modelValue', val as PaperFormDTO)
  },
  { deep: true }
)

watch(
  () => props.modelValue,
  (val) => {
    Object.assign(form, val)
  },
  { deep: true }
)

function onDoiResult(data: DoiLookupResult) {
  emit('doi-ready', data)
}

// ── Intelligent Matching (title + authors → CrossRef search) ──────────

const searchDialogVisible = ref(false)
const searchLoading = ref(false)
const searchResults = ref<LiteratureSearchItem[]>([])

async function triggerSearch() {
  if (!form.title) {
    ElMessage.warning('请先输入论文标题')
    return
  }
  if (!form.authors) {
    try {
      await ElMessageBox.confirm(
        '当前未填写作者信息，建议补充至少一位作者以提高匹配准确性。是否继续搜索？',
        '搜索提示',
        {
          confirmButtonText: '继续搜索',
          cancelButtonText: '去填作者',
          type: 'info',
        }
      )
    } catch {
      // User clicked "去填作者" or closed dialog
      return
    }
  }
  searchLoading.value = true
  searchDialogVisible.value = true
  searchResults.value = []

  try {
    const res: any = await searchLiterature({
      title: form.title,
      authors: form.authors || undefined,
    })
    // Axios interceptor unwraps to { code, data, message }
    if (res?.code === 200 && Array.isArray(res.data)) {
      searchResults.value = res.data
      if (res.data.length === 0) {
        ElMessage.info('未找到匹配结果，请调整标题或作者后重试')
      }
    } else {
      ElMessage.warning('搜索服务暂时不可用，请稍后重试')
    }
  } catch {
    ElMessage.error('文献检索失败，请检查网络连接后重试')
    searchDialogVisible.value = false
  } finally {
    searchLoading.value = false
  }
}

function onSearchResultConfirm(item: LiteratureSearchItem) {
  // Auto-fill all available fields from the selected match
  if (item.title) form.title = item.title
  if (item.authors) form.authors = item.authors
  if (item.journal) form.journal = item.journal
  if (item.doi) form.doi = item.doi
  if (item.volume) form.volume = item.volume
  if (item.issue) form.issue = item.issue
  if (item.pages) form.pages = item.pages
  if (item.publishYear) form.publishYear = item.publishYear
  if (item.abstractText) form.abstractText = item.abstractText

  ElMessage.success('已根据匹配结果自动填充表单')
}

// ── Form Field Options ────────────────────────────────────────────

const indexStatusOptions = [
  'SCI', 'SSCI', 'EI', 'CPCI', 'CSCD', 'CSSCI', '北大核心', '其他',
]

const zoneOptions = ['一区', '二区', '三区', '四区', '无']

// ── Validation Rules ──────────────────────────────────────────────

const titleRules = [
  { required: true, message: '请输入论文标题', trigger: 'blur' },
  { max: 500, message: '标题不超过500字', trigger: 'blur' },
]

const authorsRules = [
  { required: true, message: '作者不能为空', trigger: 'blur' },
]

const journalRules = [
  { required: true, message: '请输入期刊/会议名称', trigger: 'blur' },
]

const publishYearRules = [
  { required: true, message: '请选择发表年份', trigger: 'change' },
]

const indexStatusRules = [
  { required: true, message: '请选择收录情况', trigger: 'change' },
]
</script>

<style scoped>
.paper-form {
  width: 100%;
}

.title-search-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.title-input {
  flex: 1;
}

.smart-match-btn {
  flex-shrink: 0;
  white-space: nowrap;
}

.smart-match-tip {
  display: flex;
  align-items: flex-start;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.smart-match-tip .el-icon {
  flex-shrink: 0;
  margin-top: 1px;
  color: #409eff;
}
</style>
