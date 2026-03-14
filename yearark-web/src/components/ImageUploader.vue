<script setup lang="ts">
import { ref } from 'vue'
import { uploadImage } from '@/api/share'
import { useToast } from '@/components/ui/toast/use-toast'

const emit = defineEmits<{ uploaded: [] }>()
const { toast } = useToast()

const fileInput = ref<HTMLInputElement>()
const isDragging = ref(false)
const uploadingCount = ref(0)

function openFilePicker() { fileInput.value?.click() }

function onFileSelected(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.length) {
    handleFiles(Array.from(input.files))
    input.value = ''
  }
}

function onDragOver(e: DragEvent) { e.preventDefault(); isDragging.value = true }
function onDragLeave() { isDragging.value = false }

function onDrop(e: DragEvent) {
  e.preventDefault()
  isDragging.value = false
  const files = Array.from(e.dataTransfer?.files || []).filter(f => f.type.startsWith('image/'))
  if (files.length === 0) {
    toast({ description: '请拖入图片文件', variant: 'destructive' })
    return
  }
  handleFiles(files)
}

async function handleFiles(files: File[]) {
  const imageFiles = files.filter(f => f.type.startsWith('image/'))
  if (imageFiles.length === 0) {
    toast({ description: '仅支持图片文件', variant: 'destructive' })
    return
  }

  uploadingCount.value += imageFiles.length

  let successCount = 0
  let failCount = 0

  await Promise.allSettled(
    imageFiles.map(async (file) => {
      try {
        await uploadImage(file)
        successCount++
      } catch {
        failCount++
      } finally {
        uploadingCount.value--
      }
    })
  )

  if (failCount === 0) {
    toast({ description: `${successCount} 张图片上传成功` })
  } else {
    toast({ description: `成功 ${successCount} 张，失败 ${failCount} 张`, variant: 'destructive' })
  }
  emit('uploaded')
}
</script>

<template>
  <div
    class="relative flex flex-col items-center justify-center rounded-lg border-2 border-dashed aspect-square transition-colors cursor-pointer"
    :class="isDragging ? 'border-primary bg-primary/5' : 'border-muted-foreground/25 hover:border-primary/50'"
    @dragover="onDragOver" @dragleave="onDragLeave" @drop="onDrop" @click="openFilePicker"
  >
    <input ref="fileInput" type="file" accept="image/*" multiple class="hidden" @change="onFileSelected" />

    <div v-if="uploadingCount > 0" class="flex flex-col items-center gap-2">
      <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
      <span class="text-xs text-muted-foreground">上传中 ({{ uploadingCount }})</span>
    </div>
    <template v-else>
      <svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 text-muted-foreground mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
      </svg>
      <p class="text-xs text-muted-foreground text-center px-2">上传图片</p>
    </template>
  </div>
</template>
