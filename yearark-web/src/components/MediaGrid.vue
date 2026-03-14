<script setup lang="ts">
import { ref } from 'vue'
import { X, Plus, Loader2 } from 'lucide-vue-next'

export interface MediaItem {
  id: number
  albumId: number
  type: number // 1=text, 2=image
  content: string
  status: number
  sort: number
}

const props = defineProps<{
  images: MediaItem[]
  uploading?: boolean
}>()

const emit = defineEmits<{
  (e: 'delete', id: number): void
  (e: 'upload', file: File): void
}>()

const fileInput = ref<HTMLInputElement>()

function triggerUpload() {
  fileInput.value?.click()
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''
  if (!file.type.startsWith('image/')) return
  emit('upload', file)
}
</script>

<template>
  <div class="grid grid-cols-4 sm:grid-cols-5 md:grid-cols-6 gap-2">
    <!-- Upload tile -->
    <button
      type="button"
      class="aspect-square rounded-lg border-2 border-dashed border-muted-foreground/30 flex items-center justify-center bg-muted/40 hover:bg-muted/70 hover:border-primary/50 transition-colors"
      :disabled="uploading"
      @click="triggerUpload"
    >
      <Loader2 v-if="uploading" class="w-5 h-5 text-muted-foreground animate-spin" />
      <Plus v-else class="w-5 h-5 text-muted-foreground" />
    </button>
    <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="onFileChange" />

    <!-- Image tiles -->
    <div
      v-for="img in images"
      :key="img.id"
      class="relative aspect-square rounded-lg overflow-hidden border bg-muted group"
    >
      <img
        :src="img.content"
        :alt="`素材 ${img.id}`"
        class="w-full h-full object-cover"
        loading="lazy"
      />
      <button
        type="button"
        class="absolute top-1 right-1 w-5 h-5 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-destructive"
        @click.stop="emit('delete', img.id)"
      >
        <X class="w-3 h-3" />
      </button>
    </div>
  </div>

  <p v-if="images.length === 0 && !uploading" class="text-center py-4 text-muted-foreground text-sm">
    点击 + 上传图片
  </p>
</template>
