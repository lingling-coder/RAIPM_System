<template>
  <div class="patent-form">
    <el-form-item label="专利名称" prop="patentName" :rules="patentNameRules">
      <el-input v-model="form.patentName" placeholder="请输入专利名称" maxlength="500" show-word-limit />
    </el-form-item>

    <el-form-item label="发明人" prop="inventors" :rules="inventorsRules">
      <el-input v-model="form.inventors" placeholder="多个发明人请用分号分隔" />
    </el-form-item>

    <el-form-item label="申请号" prop="applicationNo" :rules="applicationNoRules">
      <el-input v-model="form.applicationNo" placeholder="请输入专利申请号" />
    </el-form-item>

    <el-form-item label="授权号" prop="authorizationNo">
      <el-input v-model="form.authorizationNo" placeholder="如有请填写" />
    </el-form-item>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-form-item label="申请日" prop="applicationDate" :rules="applicationDateRules">
          <el-date-picker
            v-model="form.applicationDate"
            type="date"
            placeholder="请选择申请日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
      <el-col :span="12">
        <el-form-item label="授权日" prop="authorizationDate">
          <el-date-picker
            v-model="form.authorizationDate"
            type="date"
            placeholder="请选择授权日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-col>
    </el-row>

    <el-form-item label="专利类型" prop="patentType" :rules="patentTypeRules">
      <el-select v-model="form.patentType" placeholder="请选择专利类型" style="width: 300px">
        <el-option v-for="opt in patentTypeOptions" :key="opt" :label="opt" :value="opt" />
      </el-select>
    </el-form-item>

    <el-form-item label="国别" prop="country" :rules="countryRules">
      <el-select v-model="form.country" placeholder="请选择国别" style="width: 300px">
        <el-option v-for="opt in countryOptions" :key="opt" :label="opt" :value="opt" />
      </el-select>
    </el-form-item>

    <el-form-item label="年费下次缴费日" prop="nextFeeDate">
      <el-date-picker
        v-model="form.nextFeeDate"
        type="date"
        placeholder="请选择下次缴费日期"
        value-format="YYYY-MM-DD"
        style="width: 200px"
      />
    </el-form-item>

    <el-form-item label="法律状态" prop="legalStatus" :rules="legalStatusRules">
      <el-select v-model="form.legalStatus" placeholder="请选择法律状态" style="width: 300px">
        <el-option v-for="opt in legalStatusOptions" :key="opt" :label="opt" :value="opt" />
      </el-select>
    </el-form-item>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import type { PatentFormDTO } from '@/api/achievement/patent'

const props = defineProps<{
  modelValue: PatentFormDTO
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: PatentFormDTO): void
}>()

const form = reactive<PatentFormDTO>({ ...props.modelValue })

watch(
  () => ({ ...form }),
  (val) => {
    emit('update:modelValue', val as PatentFormDTO)
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

const patentTypeOptions = ['发明', '实用新型', '外观设计']
const countryOptions = ['中国', '美国', '欧洲', '日本', '韩国', 'PCT', '其他']
const legalStatusOptions = ['授权', '实审', '公开', '驳回', '撤回', '终止', '无效']

// ── Validation Rules ──────────────────────────────────────────────

const patentNameRules = [
  { required: true, message: '请输入专利名称', trigger: 'blur' },
  { max: 500, message: '专利名称不超过500字', trigger: 'blur' },
]

const inventorsRules = [
  { required: true, message: '发明人不能为空', trigger: 'blur' },
]

const applicationNoRules = [
  { required: true, message: '请输入专利申请号', trigger: 'blur' },
]

const applicationDateRules = [
  { required: true, message: '请选择申请日期', trigger: 'change' },
]

const patentTypeRules = [
  { required: true, message: '请选择专利类型', trigger: 'change' },
]

const countryRules = [
  { required: true, message: '请选择国别', trigger: 'change' },
]

const legalStatusRules = [
  { required: true, message: '请选择法律状态', trigger: 'change' },
]
</script>

<style scoped>
.patent-form {
  width: 100%;
}
</style>
