<script setup lang="ts">
import { ref } from 'vue'
import { uploadImage } from '@/api/share'

const emit = defineEmits<{ uploaded: [] }>()

const fileInput = ref<HTMLInputElement>()
const isDragging = ref(false)
const uploading = ref(false)
const message = ref('')
const messageType = ref<'success' | 'error'>('')
const previewUrl = ref<string | null>(null)

function openFilePicker() { fileInput.value?.click() }

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) { handleUpload(input.files[0]); input.value = '' }
}

function onDragOver(e: DragEvent) { e.preventDefault(); isDragging.value = true }
function onDragLeave() { isDragging.value = false }

function onDrop(e: DragEvent) {
  e.preventDefault(); isDragging.value = false
  const file = e.dataTransfer?.files[0]
  if (file && file.type.startsWith('image/')) handleUpload(file)
  else showMessage('请拖入图片文件', 'error')
}

async function handleUpload(file: File) {
  if (!file.type.startsWith('image/')) { showMessage('仅支持图片文件', 'error'); return }
  previewUrl.value = URL.createObjectURL(file)
  uploading.value = true
  message.value = ''
  try {
    await uploadImage(file)
    showMessage('上传成功', 'success')
    emit('uploaded')
  } catch (err: any) {
    showMessage(err.response?.data?.msg || '上传失败', 'error')
    previewUrl.value = null
  } finally {
    uploading.value = false
  }
}

function showMessage(msg: string, type: 'success' | 'error') {
  message.value = msg; messageType.value = type
  setTimeout(() => { message.value = '' }, 3000)
}
</script>

<template>
  <div>
    <div
      class="relative flex flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 sm:p-10 transition-colors cursor-pointer"
      :class="isDragging ? 'border-primary bg-primary/5' : 'border-muted-foreground/25 hover:border-primary/50'"
      @dragover="onDragOver" @dragleave="onDragLeave" @drop="onDrop" @click="openFilePicker"
    >
      <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="onFileSelected" />
      <svg xmlns="http://www.w3.org/2000/svg" class="h-10 w-10 text-muted-foreground mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
      </svg>
      <p class="text-sm text-muted-foreground text-center">
        <span class="font-medium text-primary">点击上传</span> 或拖拽图片到此处
      </p>
      <p class="text-xs text-muted-foreground mt-1">支持 JPG、PNG、GIF 等图片格式</p>
      <div v-if="uploading" class="absolute inset-0 flex items-center justify-center rounded-lg bg-background/80">
        <span class="text-sm text-muted-foreground">上传中...</span>
      </div>
    </div>
    <p v-if="message" class="mt-2 text-sm text-center" :class="messageType === 'success' ? 'text-green-600' : 'text-destructive'">{{ message }}</p>
    <div v-if="previewUrl" class="mt-3 flex justify-center">
      <div class="relative w-32 h-32 rounded-md overflow-hidden border">
        <img :src="previewUrl" alt="预览" class="h-full w-full object-cover" />
        <div v-if="uploading" class="absolute inset-0 flex items-center justify-center bg-background/60">
          <span class="text-xs text-muted-foreground">上传中...</span>
        </div>
      </div>
    </div>
  </div>
</template>
