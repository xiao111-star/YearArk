<script setup lang="ts">
export interface UnusedMedia {
  id: number
  content: string
  sort: number
}

defineProps<{ media: UnusedMedia[] }>()
defineEmits<{ 'drag-start': [media: UnusedMedia] }>()
</script>

<template>
  <div class="p-3 border rounded-lg bg-muted/30">
    <p class="text-xs font-medium text-muted-foreground mb-2">未使用素材（{{ media.length }}）</p>
    <div v-if="media.length === 0" class="text-xs text-muted-foreground">暂无未使用素材</div>
    <div class="grid grid-cols-3 gap-2">
      <div
        v-for="item in media"
        :key="item.id"
        draggable="true"
        class="aspect-square rounded overflow-hidden cursor-grab active:cursor-grabbing border hover:ring-2 hover:ring-primary"
        @dragstart="$emit('drag-start', item)"
      >
        <img :src="item.content" :alt="`素材${item.id}`" class="w-full h-full object-cover" />
      </div>
    </div>
  </div>
</template>
