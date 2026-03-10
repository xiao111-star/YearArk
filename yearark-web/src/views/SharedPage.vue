<script setup lang="ts">
import { Share2 } from 'lucide-vue-next'
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-[60vh] text-center space-y-4">
    <div class="w-16 h-16 rounded-full bg-muted flex items-center justify-center">
      <Share2 class="w-8 h-8 text-muted-foreground" />
    </div>
    <h2 class="text-2xl font-serif font-bold">我的分享</h2>
    <p class="text-muted-foreground max-w-md">
      查看和管理您分享给他人的纪念册链接。
    </p>
  </div>
</template>
