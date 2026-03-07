<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  try {
    await logoutApi()
  } catch {
    // 即使后端调用失败，也清除本地状态
  } finally {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="min-h-screen flex flex-col bg-background">
    <header class="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div class="container mx-auto flex h-14 items-center justify-between px-4">
        <div class="flex items-center gap-6">
          <router-link to="/dashboard" class="text-xl font-bold text-primary">
            YearArk
          </router-link>
          <nav class="flex items-center gap-4 text-sm">
            <router-link
              to="/dashboard"
              class="text-muted-foreground transition-colors hover:text-foreground"
              active-class="text-foreground font-medium"
            >
              首页
            </router-link>
          </nav>
        </div>
        <div class="flex items-center gap-4">
          <span class="text-sm text-muted-foreground">{{ userStore.username }}</span>
          <button
            class="text-sm text-muted-foreground transition-colors hover:text-foreground"
            @click="handleLogout"
          >
            退出登录
          </button>
        </div>
      </div>
    </header>
    <main class="flex-1 container mx-auto px-4 py-6">
      <router-view />
    </main>
  </div>
</template>
