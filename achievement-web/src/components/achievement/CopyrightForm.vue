<template>
  <div class="copyright-form">
    <el-form-item label="软著名称" prop="name" :rules="nameRules">
      <el-input v-model="form.name" placeholder="请输入软著名称" maxlength="500" show-word-limit />
    </el-form-item>

    <el-form-item label="著作权人" prop="copyrightHolder" :rules="copyrightHolderRules">
      <el-input v-model="form.copyrightHolder" placeholder="请输入著作权人" />
    </el-form-item>

    <el-form-item label="登记号" prop="registrationNo" :rules="registrationNoRules">
      <el-input v-model="form.registrationNo" placeholder="请输入登记号" />
    </el-form-item>

    <el-form-item label="登记日期" prop="registrationDate" :rules="registrationDateRules">
      <el-date-picker
        v-model="form.registrationDate"
        type="date"
        placeholder="请选择登记日期"
        value-format="YYYY-MM-DD"
        style="width: 200px"
      />
    </el-form-item>

    <el-form-item label="版本号" prop="softwareVersion" :rules="softwareVersionRules">
      <el-input v-model="form.softwareVersion" placeholder="请输入版本号" />
    </el-form-item>

    <el-form-item label="软件类别" prop="softwareCategory" :rules="softwareCategoryRules">
      <el-select v-model="form.softwareCategory" placeholder="请选择软件类别" style="width: 300px">
        <el-option v-for="opt in softwareCategoryOptions" :key="opt" :label="opt" :value="opt" />
      </el-select>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { CopyrightFormDTO } from '@/api/achievement/copyright'

const props = defineProps<{
  modelValue: CopyrightFormDTO
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: CopyrightFormDTO): void
}>()

const form = reactive<CopyrightFormDTO>({ ...props.modelValue })

watch(
  () => ({ ...form }),
  (val) => {
    emit('update:modelValue', val as CopyrightFormDTO)
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

// ── Form Field Options ────────────────────────────────────────────

const softwareCategoryOptions = ['操作系统', '数据库', '中间件', '应用软件', '嵌入式软件', '其他']

// ── Validation Rules ──────────────────────────────────────────────

const nameRules = [
  { required: true, message: '请输入软著名称', trigger: 'blur' },
  { max: 500, message: '软著名称不超过500字', trigger: 'blur' },
]

const copyrightHolderRules = [
  { required: true, message: '著作权人不能为空', trigger: 'blur' },
]

const registrationNoRules = [
  { required: true, message: '请输入登记号', trigger: 'blur' },
]

const registrationDateRules = [
  { required: true, message: '请选择登记日期', trigger: 'change' },
]

const softwareVersionRules = [
  { required: true, message: '请输入版本号', trigger: 'blur' },
]

const softwareCategoryRules = [
  { required: true, message: '请选择软件类别', trigger: 'change' },
]
</script>

<style scoped>
.copyright-form {
  width: 100%;
}
</style>
