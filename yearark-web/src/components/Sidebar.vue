<script setup lang="ts">
import {
  LayoutDashboard,
  Book,
  LayoutTemplate,
  Share2,
  LogOut,
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'
import { useRouter } from 'vue-router'
import { cn } from '@/lib/utils'

const router = useRouter()
const userStore = useUserStore()

const menuItems = [
  { icon: LayoutDashboard, label: '概览', path: '/dashboard' },
  { icon: Book, label: '我的纪念册', path: '/albums' }, // Assuming we split dashboard
  { icon: LayoutTemplate, label: '模版中心', path: '/templates' },
  { icon: Share2, label: '我的分享', path: '/shared' },
]

async function handleLogout() {
  try {
    await logoutApi()
  } finally {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <aside class="w-64 border-r bg-card flex flex-col h-screen fixed left-0 top-0 z-30 transition-all duration-300">
    <div class="h-16 flex items-center px-6 border-b">
      <router-link to="/dashboard" class="flex items-center gap-2 font-serif text-2xl font-bold text-primary">
        <Book class="w-6 h-6" />
        <span>YearArk</span>
      </router-link>
    </div>

    <div class="flex-1 py-6 px-4 space-y-1">
      <template v-for="item in menuItems" :key="item.path">
        <router-link :to="item.path" v-slot="{ isActive }">
          <Button
            variant="ghost"
            :class="cn('w-full justify-start gap-3 mb-1', isActive && 'bg-accent text-accent-foreground font-medium')"
          >
            <component :is="item.icon" class="w-4 h-4" />
            {{ item.label }}
          </Button>
        </router-link>
      </template>
    </div>

    <div class="p-4 border-t space-y-2">
      <div class="px-4 py-2 flex items-center gap-3 text-sm text-muted-foreground mb-2">
        <div class="w-8 h-8 rounded-full bg-secondary flex items-center justify-center text-foreground font-medium">
          {{ userStore.username?.[0]?.toUpperCase() || 'U' }}
        </div>
        <div class="flex flex-col overflow-hidden">
          <span class="truncate font-medium text-foreground">{{ userStore.username }}</span>
          <span class="text-xs">Pro Member</span>
        </div>
      </div>
      
      <Button variant="ghost" class="w-full justify-start gap-3 text-muted-foreground hover:text-destructive" @click="handleLogout">
        <LogOut class="w-4 h-4" />
        退出登录
      </Button>
    </div>
  </aside>
</template>
