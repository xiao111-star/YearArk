<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const albumName = (route.query.albumName as string) || '纪念册'
</script>

<template>
  <div class="flex flex-col items-center justify-center py-20 text-center space-y-6">
    <!-- 成功图标 -->
    <div class="w-20 h-20 rounded-full bg-green-100 flex items-center justify-center">
      <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
      </svg>
    </div>

    <div class="space-y-2">
      <h1 class="text-2xl font-bold">提交成功</h1>
      <p class="text-muted-foreground">感谢你为「{{ albumName }}」贡献的珍贵回忆</p>
    </div>

    <p class="text-sm text-muted-foreground max-w-sm">
      你上传的照片和留言已成功提交，纪念册的主人会收到你的内容。
    </p>
  </div>
</template>
