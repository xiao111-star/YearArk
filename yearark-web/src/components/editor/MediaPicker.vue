<script setup lang="ts">
import { ref, watch } from 'vue'
import { X, Upload, Loader2, ImagePlus } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { getUnusedMedia, uploadAlbumMedia } from '@/api/album'

interface MediaItem {
  id: number
  content: string
}

const props = defineProps<{
  albumId: number
  visible: boolean
}>()

const emit = defineEmits<{
  pick: [media: { id: number; url: string }]
  close: []
}>()

const mediaList = ref<MediaItem[]>([])
const loading = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

async function loadMedia() {
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

watch(() => props.visible, (val) => {
  if (val) loadMedia()
})

function openFilePicker() {
  fileInput.value?.click()
}

async function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  input.value = ''

  if (!file.type.startsWith('image/')) return

  uploading.value = true
  try {
    const res = await uploadAlbumMedia(props.albumId, file)
    const newMedia = (res.data?.data ?? res.data) as MediaItem
    if (newMedia) {
      mediaList.value.unshift(newMedia)
    }
  } finally {
    uploading.value = false
  }
}

function selectMedia(item: MediaItem) {
  emit('pick', { id: item.id, url: item.content })
}

function close() {
  emit('close')
}

function onBackdropClick(e: MouseEvent) {
  if (e.target === e.currentTarget) close()
}
</script>

<template>
  <Teleport to="body">
    <div
      v-if="visible"
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click="onBackdropClick"
    >
      <div class="bg-background rounded-lg shadow-xl w-full max-w-2xl max-h-[80vh] flex flex-col mx-4">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b">
          <h3 class="text-lg font-medium">选择图片</h3>
          <button class="text-muted-foreground hover:text-foreground" @click="close">
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- Upload area -->
        <div class="px-5 pt-4">
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            class="hidden"
            @change="onFileSelected"
          />
          <Button
            variant="outline"
            class="w-full"
            :disabled="uploading"
            @click="openFilePicker"
          >
            <Loader2 v-if="uploading" class="w-4 h-4 mr-2 animate-spin" />
            <Upload v-else class="w-4 h-4 mr-2" />
            {{ uploading ? '上传中...' : '上传图片' }}
          </Button>
        </div>

        <!-- Media grid -->
        <div class="flex-1 overflow-y-auto px-5 py-4">
          <!-- Loading -->
          <div v-if="loading" class="flex items-center justify-center py-12">
            <Loader2 class="w-6 h-6 animate-spin text-muted-foreground" />
          </div>

          <!-- Empty -->
          <div
            v-else-if="mediaList.length === 0"
            class="flex flex-col items-center justify-center py-12 text-muted-foreground"
          >
            <ImagePlus class="w-10 h-10 mb-2" />
            <p class="text-sm">暂无图片素材，请先上传</p>
          </div>

          <!-- Grid -->
          <div v-else class="grid grid-cols-3 sm:grid-cols-4 gap-3">
            <div
              v-for="item in mediaList"
              :key="item.id"
              class="aspect-square rounded-lg overflow-hidden border cursor-pointer hover:ring-2 hover:ring-primary transition-shadow"
              @click="selectMedia(item)"
            >
              <img
                :src="item.content"
                :alt="`素材 ${item.id}`"
                class="w-full h-full object-cover"
                loading="lazy"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
