<script setup lang="ts">
import Navbar from '@/components/Navbar.vue'
</script>

<template>
  <div class="min-h-screen bg-background font-sans text-foreground">
    <Navbar />
    <main class="min-h-[calc(100vh-4rem)]">
      <div class="container mx-auto p-6 max-w-7xl animate-in fade-in duration-500">
        <router-view />
      </div>
    </main>
  </div>
</template>
