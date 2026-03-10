<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import Sidebar from '@/components/Sidebar.vue'

const router = useRouter()
const userStore = useUserStore()
</script>

<template>
  <div class="min-h-screen bg-background font-sans text-foreground">
    <!-- Sidebar -->
    <Sidebar class="hidden md:flex" />

    <!-- Mobile Header (Visible only on small screens) -->
    <header class="md:hidden sticky top-0 z-50 border-b bg-background/95 backdrop-blur px-4 h-14 flex items-center justify-between">
      <span class="text-xl font-serif font-bold text-primary">YearArk</span>
      <!-- Add mobile menu toggle here if needed -->
    </header>

    <!-- Main Content -->
    <main class="md:ml-64 min-h-screen transition-all duration-300">
      <div class="container mx-auto p-6 max-w-7xl animate-in fade-in duration-500">
        <router-view />
      </div>
    </main>
  </div>
</template>
