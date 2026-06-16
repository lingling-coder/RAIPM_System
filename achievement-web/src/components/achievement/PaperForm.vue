<template>
  <div class="paper-form">
    <el-form-item label="论文标题" prop="title" :rules="titleRules">
      <el-input v-model="form.title" placeholder="请输入论文标题" maxlength="500" show-word-limit />
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
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { PaperFormDTO, DoiLookupResult } from '@/api/achievement/paper'
import DoiAutoComplete from './DoiAutoComplete.vue'

const props = defineProps<{
  modelValue: PaperFormDTO
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: PaperFormDTO): void
  (e: 'doi-ready', data: DoiLookupResult): void
}>()

const form = reactive<PaperFormDTO>({ ...props.modelValue })

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
</style>
