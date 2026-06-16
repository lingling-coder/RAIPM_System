<template>
  <div class="profile-page">
    <!-- Loading state -->
    <template v-if="loading">
      <el-skeleton :rows="5" animated />
    </template>

    <!-- Error state -->
    <template v-else-if="error">
      <el-alert title="数据加载失败" type="error" show-icon>
        <template #default>
          <el-button type="text" @click="fetchProfile">点击重试</el-button>
        </template>
      </el-alert>
    </template>

    <!-- Content -->
    <template v-else>
      <!-- Card 1: Personal Info -->
      <el-card shadow="never" class="profile-card">
        <template #header>
          <div class="card-header">
            <span>个人信息</span>
            <el-button type="primary" size="small" @click="editDrawerVisible = true">
              编辑信息
            </el-button>
          </div>
        </template>

        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名" min-width="120">
            {{ profile?.username || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="姓名">
            {{ profile?.realName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="部门">
            {{ profile?.deptName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag
              v-for="role in profile?.roleNames"
              :key="role"
              size="small"
              effect="plain"
              class="role-tag"
            >
              {{ role }}
            </el-tag>
            <span v-if="!profile?.roleNames?.length">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">
            {{ profile?.phone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="邮箱">
            {{ profile?.email || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="最后登录时间" :span="2">
            {{ profile?.lastLoginTime || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Card 2: Change Password -->
      <el-card shadow="never" class="profile-card">
        <template #header>
          <span>修改密码</span>
        </template>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-width="100px"
          class="password-form"
        >
          <el-form-item label="当前密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="至少8位，包含字母和数字"
            />
          </el-form-item>

          <!-- Password strength indicator -->
          <el-form-item label="密码强度">
            <el-progress
              :percentage="passwordStrength.percentage"
              :color="passwordStrength.color"
              :stroke-width="8"
              :show-text="false"
              class="strength-bar"
            />
            <span class="strength-text" :style="{ color: passwordStrength.color }">
              {{ passwordStrength.label }}
            </span>
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
              修改密码
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <!-- Edit Profile Drawer -->
    <el-drawer
      v-model="editDrawerVisible"
      title="编辑信息"
      size="450px"
      direction="rtl"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="editForm.realName" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="handleEditProfile">
          保存
        </el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getProfile, updateProfile, changePassword, type ProfileInfo, type ProfileUpdateData } from '@/api/profile'

const router = useRouter()
const userStore = useUserStore()

// ── Profile Data ───────────────────────────────────────────────────────
const loading = ref(false)
const error = ref(false)
const profile = ref<ProfileInfo | null>(null)

async function fetchProfile() {
  loading.value = true
  error.value = false
  try {
    const res = await getProfile()
    if (res.code === 200 && res.data) {
      profile.value = res.data
    }
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

onMounted(fetchProfile)

// ── Edit Profile ───────────────────────────────────────────────────────
const editDrawerVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref<FormInstance>()

const editForm = reactive<ProfileUpdateData>({
  realName: '',
  email: '',
  phone: '',
})

const editRules: FormRules = {
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

async function handleEditProfile() {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) return

  editLoading.value = true
  try {
    await updateProfile(editForm)
    ElMessage.success('个人信息更新成功')
    editDrawerVisible.value = false
    await fetchProfile()
  } catch {
    ElMessage.error('保存失败，请稍后重试')
  } finally {
    editLoading.value = false
  }
}

// ── Change Password ────────────────────────────────────────────────────
const passwordLoading = ref(false)
const passwordFormRef = ref<FormInstance>()

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度不能少于8位', trigger: 'blur' },
    {
      pattern: /^(?=.*[a-zA-Z])(?=.*\d).+$/,
      message: '密码必须包含字母和数字',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

// Password strength indicator
const passwordStrength = computed(() => {
  const pwd = passwordForm.newPassword
  if (!pwd) return { percentage: 0, color: '#909399', label: '未设置' }
  if (pwd.length < 8) return { percentage: 30, color: '#f56c6c', label: '弱' }

  let score = 0
  if (/[a-z]/.test(pwd)) score++
  if (/[A-Z]/.test(pwd)) score++
  if (/\d/.test(pwd)) score++
  if (/[^a-zA-Z0-9]/.test(pwd)) score++

  if (score >= 3 && pwd.length >= 10) return { percentage: 100, color: '#67c23a', label: '强' }
  if (score >= 2) return { percentage: 65, color: '#e6a23c', label: '中' }
  return { percentage: 30, color: '#f56c6c', label: '弱' }
})

async function handleChangePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')

    // Clear token state and redirect to login
    userStore.resetState()
    router.push('/login')
  } catch (error: any) {
    const msg = error?.response?.data?.message || '密码修改失败'
    ElMessage.error(msg)
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-page {
  max-width: 800px;
  margin: 0 auto;

  .profile-card {
    margin-bottom: 16px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .role-tag {
      margin-right: 4px;
      margin-bottom: 4px;
    }
  }

  .password-form {
    max-width: 500px;

    .strength-bar {
      width: 200px;
      margin-right: 12px;
    }

    .strength-text {
      font-size: 12px;
    }
  }
}
</style>
