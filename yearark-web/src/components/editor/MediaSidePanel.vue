<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { Loader2, ImagePlus } from 'lucide-vue-next'
import { getUnusedMedia } from '@/api/album'

interface MediaItem {
  id: number
  content: string
}

const props = defineProps<{
  albumId: number
}>()

const emit = defineEmits<{
  pick: [media: { id: number; url: string }]
}>()

const mediaList = ref<MediaItem[]>([])
const loading = ref(false)

async function loadMedia() {
  if (!props.albumId) return
  loading.value = true
  try {
    const res = await getUnusedMedia(props.albumId)
    mediaList.value = (res.data?.data ?? res.data ?? []) as MediaItem[]
  } catch {
    mediaList.value = []
  } finally {
    loading.value = false
  }
}

watch(() => props.albumId, (val) => {
  if (val) loadMedia()
})

onMounted(() => {
  loadMedia()
})

function selectMedia(item: MediaItem) {
  emit('pick', { id: item.id, url: item.content })
}
</script>

<template>
  <div class="h-full flex flex-col bg-background border-l">
    <!-- Header -->
    <div class="px-4 py-3 border-b bg-muted/30">
      <h3 class="text-sm font-medium text-foreground">选择替换图片</h3>
      <p class="text-xs text-muted-foreground mt-0.5">点击图片即可替换当前选中位置</p>
    </div>

    <!-- Media grid -->
    <div class="flex-1 overflow-y-auto p-4 custom-scrollbar">
      <!-- Loading -->
      <div v-if="loading" class="flex flex-col items-center justify-center py-12 gap-2">
        <Loader2 class="w-5 h-5 animate-spin text-muted-foreground" />
        <span class="text-xs text-muted-foreground">加载素材中...</span>
      </div>

      <!-- Empty -->
      <div
        v-else-if="mediaList.length === 0"
        class="flex flex-col items-center justify-center py-12 text-muted-foreground px-4 text-center"
      >
        <ImagePlus class="w-8 h-8 mb-2 opacity-50" />
        <p class="text-xs">暂无图片素材</p>
        <p class="text-xs mt-1 opacity-70">请在详情页上传素材后在此使用</p>
      </div>

      <!-- Grid -->
      <div v-else class="grid grid-cols-2 gap-3">
        <div
          v-for="item in mediaList"
          :key="item.id"
          class="aspect-square rounded-md overflow-hidden border bg-muted cursor-pointer hover:ring-2 hover:ring-primary/50 hover:border-primary transition-all group relative"
          @click="selectMedia(item)"
        >
          <img
            :src="item.content"
            :alt="`素材 ${item.id}`"
            class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
            loading="lazy"
          />
          <!-- Hover overlay -->
          <div class="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors" />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 5px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: hsl(var(--muted-foreground) / 0.2);
  border-radius: 10px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: hsl(var(--muted-foreground) / 0.4);
}
</style>
