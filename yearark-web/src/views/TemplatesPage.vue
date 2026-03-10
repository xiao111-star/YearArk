<script setup lang="ts">
import { LayoutTemplate } from 'lucide-vue-next'
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-[60vh] text-center space-y-4">
    <div class="w-16 h-16 rounded-full bg-muted flex items-center justify-center">
      <LayoutTemplate class="w-8 h-8 text-muted-foreground" />
    </div>
    <h2 class="text-2xl font-serif font-bold">模版中心</h2>
    <p class="text-muted-foreground max-w-md">
      更多精美纪念册模版正在设计中，敬请期待...
    </p>
  </div>
</template>
