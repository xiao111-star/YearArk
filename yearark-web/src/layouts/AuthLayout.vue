<script setup lang="ts">
import Navbar from '@/components/Navbar.vue'
</script>

<template>
  <div class="min-h-screen bg-muted/40 font-sans text-foreground">
    <Navbar />
    <div class="flex items-center justify-center px-4 py-16 min-h-[calc(100vh-4rem)]">
      <div class="w-full max-w-md">
        <router-view />
      </div>
    </div>
  </div>
</template>
