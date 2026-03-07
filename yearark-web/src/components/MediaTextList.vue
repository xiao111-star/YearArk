<script setup lang="ts">
import type { MediaItem } from '@/components/MediaGrid.vue'

defineProps<{
  texts: MediaItem[]
}>()
</script>

<template>
  <div v-if="texts.length === 0" class="text-center py-8 text-muted-foreground text-sm">
    暂无文字素材
  </div>
  <div v-else class="space-y-2">
    <div
      v-for="item in texts"
      :key="item.id"
      class="rounded-lg border p-3 text-sm leading-relaxed"
    >
      {{ item.content }}
    </div>
  </div>
</template>
