<template>
  <div class="login-page">
    <el-card class="login-card">
      <h2 class="login-title">渔具秒杀商城</h2>
      <p class="muted login-tip">演示账号：buyer1 / 123456（买家） · admin / 123456（运营）</p>
      <el-tabs v-model="activeTab">
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" label-position="top" @keyup.enter="onLogin">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" clearable />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" show-password />
            </el-form-item>
            <el-button type="primary" class="full-btn" :loading="loading" @click="onLogin">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form ref="regFormRef" :model="regForm" :rules="registerRules" label-position="top">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="regForm.username" placeholder="4-20 位字母数字" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="6-20 位" show-password />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="regForm.nickname" placeholder="昵称" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="regForm.phone" placeholder="11 位手机号" />
            </el-form-item>
            <el-button type="success" class="full-btn" :loading="loading" @click="onRegister">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { register } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref(null)
const regFormRef = ref(null)

const loginForm = reactive({ username: 'buyer1', password: '123456' })
const regForm = reactive({ username: '', password: '', nickname: '', phone: '' })

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const registerRules = {
  username: [{ required: true, min: 4, max: 20, message: '用户名 4-20 位', trigger: 'blur' }],
  password: [{ required: true, min: 6, max: 20, message: '密码 6-20 位', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [{ pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

function redirectAfterLogin() {
  const target = route.query.redirect
  router.replace(target ? String(target) : '/')
}

async function onLogin() {
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login({ username: loginForm.username, password: loginForm.password })
    ElMessage.success('登录成功')
    redirectAfterLogin()
  } finally {
    loading.value = false
  }
}

async function onRegister() {
  const valid = await regFormRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await register({ ...regForm })
    ElMessage.success('注册成功，正在为你登录')
    await userStore.login({ username: regForm.username, password: regForm.password })
    redirectAfterLogin()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #1f2d3d, #3a5169);
}

.login-card {
  width: 380px;
}

.login-title {
  margin: 0 0 4px;
  text-align: center;
}

.login-tip {
  margin: 0 0 12px;
  text-align: center;
  font-size: 12px;
}

.full-btn {
  width: 100%;
}
</style>
