<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import type { YaUserDto } from '@/types/user'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'

const router = useRouter()

const form = ref<YaUserDto>({
  username: '',
  passwordHash: '',
  email: '',
})
const loading = ref(false)
const errorMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''

  if (!form.value.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!form.value.passwordHash) {
    errorMsg.value = '请输入密码'
    return
  }
  if (form.value.passwordHash.length < 6) {
    errorMsg.value = '密码长度不能少于6位'
    return
  }
  if (!form.value.email?.trim()) {
    errorMsg.value = '请输入邮箱'
    return
  }

  loading.value = true
  try {
    await register({
      username: form.value.username.trim(),
      passwordHash: form.value.passwordHash,
      email: form.value.email.trim(),
    })
    router.push('/login')
  } catch (err: any) {
    errorMsg.value = err.response?.data?.msg || '注册失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Card>
    <CardHeader class="text-center">
      <CardTitle>注册</CardTitle>
      <CardDescription>创建你的 YearArk 账号</CardDescription>
    </CardHeader>
    <CardContent>
      <form @submit.prevent="handleRegister" class="space-y-4">
        <div class="space-y-2">
          <Label for="username">用户名</Label>
          <Input
            id="username"
            v-model="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>
        <div class="space-y-2">
          <Label for="password">密码</Label>
          <Input
            id="password"
            v-model="form.passwordHash"
            type="password"
            placeholder="请输入密码（至少6位）"
            autocomplete="new-password"
          />
        </div>
        <div class="space-y-2">
          <Label for="email">邮箱</Label>
          <Input
            id="email"
            v-model="form.email"
            type="email"
            placeholder="请输入邮箱"
            autocomplete="email"
          />
        </div>
        <p v-if="errorMsg" class="text-sm text-destructive">{{ errorMsg }}</p>
        <Button type="submit" class="w-full" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </Button>
      </form>
    </CardContent>
    <CardFooter class="justify-center">
      <p class="text-sm text-muted-foreground">
        已有账号？
        <router-link to="/login" class="text-primary hover:underline">立即登录</router-link>
      </p>
    </CardFooter>
  </Card>
</template>
