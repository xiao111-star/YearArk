<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/auth'
import type { YaUserDto } from '@/types/user'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = ref<YaUserDto>({
  username: '',
  passwordHash: '',
})
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''

  if (!form.value.username.trim()) {
    errorMsg.value = '请输入用户名'
    return
  }
  if (!form.value.passwordHash) {
    errorMsg.value = '请输入密码'
    return
  }

  loading.value = true
  try {
    const res = await login({
      username: form.value.username.trim(),
      passwordHash: form.value.passwordHash,
    })
    const { token, userId, username } = res.data.data
    userStore.setLoginInfo(token, userId, username)

    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (err: any) {
    errorMsg.value = err.response?.data?.msg || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Card>
    <CardHeader class="text-center">
      <CardTitle>登录</CardTitle>
      <CardDescription>输入你的账号信息登录 YearArk</CardDescription>
    </CardHeader>
    <CardContent>
      <form @submit.prevent="handleLogin" class="space-y-4">
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
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </div>
        <p v-if="errorMsg" class="text-sm text-destructive">{{ errorMsg }}</p>
        <Button type="submit" class="w-full" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </Button>
      </form>
    </CardContent>
    <CardFooter class="justify-center">
      <p class="text-sm text-muted-foreground">
        还没有账号？
        <router-link to="/register" class="text-primary hover:underline">立即注册</router-link>
      </p>
    </CardFooter>
  </Card>
</template>
