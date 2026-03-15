<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Book, LayoutDashboard, LayoutTemplate, Share2,
  User, LogOut, ChevronDown, Menu, X
} from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const showUserMenu = ref(false)
const mobileMenuOpen = ref(false)

const navItems = [
  { label: '我的纪念册', path: '/dashboard', auth: true, icon: LayoutDashboard },
  { label: '模板中心', path: '/templates', auth: true, icon: LayoutTemplate },
  { label: '我的分享', path: '/shared', auth: true, icon: Share2 },
]

const visibleNavItems = computed(() =>
  navItems.filter(item => !item.auth || userStore.isLoggedIn)
)

function isActive(path: string) {
  return route.path.startsWith(path)
}

async function handleLogout() {
  showUserMenu.value = false
  try {
    await logoutApi()
  } finally {
    userStore.logout()
    router.push('/')
  }
}

function closeMenus() {
  showUserMenu.value = false
  mobileMenuOpen.value = false
}
</script>

<template>
  <header class="sticky top-0 z-50 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
    <div class="container mx-auto max-w-7xl flex items-center justify-between h-16 px-6">
      <!-- Logo -->
      <router-link to="/" class="flex items-center gap-2 font-serif text-2xl font-bold text-primary shrink-0">
        <Book class="w-6 h-6" />
        <span>YearArk</span>
      </router-link>

      <!-- Desktop Nav Links -->
      <nav class="hidden md:flex items-center gap-1">
        <router-link
          v-for="item in visibleNavItems"
          :key="item.path"
          :to="item.path"
          @click="closeMenus"
          class="px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          :class="isActive(item.path)
            ? 'bg-accent text-primary'
            : 'text-muted-foreground hover:text-foreground hover:bg-accent/50'"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <!-- Right Side -->
      <div class="flex items-center gap-3">
        <!-- User Menu (logged in) -->
        <div v-if="userStore.isLoggedIn" class="relative">
          <button
            class="flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium hover:bg-accent transition-colors"
            @click="showUserMenu = !showUserMenu"
          >
            <div class="w-7 h-7 rounded-full bg-primary/10 flex items-center justify-center text-primary font-serif font-bold text-xs">
              {{ userStore.username?.[0]?.toUpperCase() || 'U' }}
            </div>
            <span class="hidden sm:inline text-foreground">{{ userStore.username }}</span>
            <ChevronDown class="w-3.5 h-3.5 text-muted-foreground" />
          </button>
          <!-- Dropdown -->
          <Transition
            enter-active-class="transition ease-out duration-100"
            enter-from-class="opacity-0 scale-95"
            enter-to-class="opacity-100 scale-100"
            leave-active-class="transition ease-in duration-75"
            leave-from-class="opacity-100 scale-100"
            leave-to-class="opacity-0 scale-95"
          >
            <div
              v-if="showUserMenu"
              class="absolute right-0 mt-2 w-48 rounded-xl border bg-card shadow-lg py-1 z-50"
            >
              <router-link
                to="/profile"
                class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-muted-foreground hover:text-foreground hover:bg-accent transition-colors"
                @click="showUserMenu = false"
              >
                <User class="w-4 h-4" />
                个人中心
              </router-link>
              <div class="border-t my-1" />
              <button
                class="w-full flex items-center gap-3 px-4 py-2.5 text-sm text-muted-foreground hover:text-destructive hover:bg-accent transition-colors"
                @click="handleLogout"
              >
                <LogOut class="w-4 h-4" />
                退出登录
              </button>
            </div>
          </Transition>
          <!-- Click outside to close -->
          <div v-if="showUserMenu" class="fixed inset-0 z-40" @click="showUserMenu = false" />
        </div>

        <!-- Guest buttons -->
        <template v-else>
          <Button variant="ghost" size="sm" @click="router.push('/login')">登录</Button>
          <Button size="sm" @click="router.push('/register')">注册</Button>
        </template>

        <!-- Mobile menu toggle -->
        <button class="md:hidden p-2 rounded-lg hover:bg-accent transition-colors" @click="mobileMenuOpen = !mobileMenuOpen">
          <X v-if="mobileMenuOpen" class="w-5 h-5" />
          <Menu v-else class="w-5 h-5" />
        </button>
      </div>
    </div>

    <!-- Mobile Nav -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div v-if="mobileMenuOpen" class="md:hidden border-t bg-background px-6 py-4 space-y-1">
        <router-link
          v-for="item in visibleNavItems"
          :key="item.path"
          :to="item.path"
          @click="mobileMenuOpen = false"
          class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors"
          :class="isActive(item.path)
            ? 'bg-accent text-primary'
            : 'text-muted-foreground hover:text-foreground hover:bg-accent/50'"
        >
          <component :is="item.icon" class="w-4 h-4" />
          {{ item.label }}
        </router-link>
      </div>
    </Transition>
  </header>
</template>
