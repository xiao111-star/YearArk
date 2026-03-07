<script setup lang="ts">
export interface MediaItem {
  id: number
  albumId: number
  type: number // 1=text, 2=image
  content: string
  status: number
  sort: number
}

defineProps<{
  images: MediaItem[]
}>()
</script>

<template>
  <div v-if="images.length === 0" class="text-center py-8 text-muted-foreground text-sm">
    暂无图片素材
  </div>
  <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
    <div
      v-for="img in images"
      :key="img.id"
      class="aspect-square rounded-lg overflow-hidden border bg-muted"
    >
      <img
        :src="img.content"
        :alt="`素材 ${img.id}`"
        class="w-full h-full object-cover"
        loading="lazy"
      />
    </div>
  </div>
</template>
