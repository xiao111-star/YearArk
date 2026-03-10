<script setup lang="ts">
import { BookOpen, Clock, Trash2 } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'

export interface Album {
  id: number
  name: string
  des: string
  status: number // 0=草稿, 1=已发布
  createTime: string
}

const props = defineProps<{
  album: Album
}>()

const emit = defineEmits<{
  detail: [id: number]
  delete: [id: number]
}>()

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function handleDelete(e: Event) {
  e.stopPropagation()
  emit('delete', props.album.id)
}
</script>

<template>
  <div
    class="group relative flex flex-col h-[320px] bg-card rounded-xl border shadow-sm hover:shadow-md transition-all overflow-hidden cursor-pointer"
    @click="emit('detail', props.album.id)"
  >
    <!-- Cover Area -->
    <div class="h-48 bg-muted relative overflow-hidden">
      <!-- Pattern / Gradient -->
      <div class="absolute inset-0 bg-gradient-to-br from-primary/5 to-primary/20" />
      <div class="absolute inset-0 flex items-center justify-center">
         <BookOpen class="w-12 h-12 text-primary/20" />
      </div>
      
      <!-- Status Badge -->
      <div class="absolute top-3 right-3">
        <span 
          class="px-2.5 py-0.5 text-xs font-medium rounded-full backdrop-blur-sm"
          :class="props.album.status === 1 ? 'bg-green-100/80 text-green-700' : 'bg-white/80 text-gray-600'"
        >
          {{ props.album.status === 1 ? '已发布' : '草稿' }}
        </span>
      </div>
    </div>

    <!-- Content Area -->
    <div class="flex-1 p-4 flex flex-col justify-between bg-card relative">
        <!-- Spine decoration -->
        <div class="absolute left-0 top-0 bottom-0 w-1 bg-primary/10 group-hover:bg-primary transition-colors"></div>

        <div class="pl-2">
            <h3 class="font-serif font-bold text-lg text-foreground line-clamp-1 group-hover:text-primary transition-colors">
              {{ props.album.name }}
            </h3>
            <p class="text-sm text-muted-foreground line-clamp-2 mt-1 h-10">
              {{ props.album.des || '暂无描述...' }}
            </p>
        </div>
        
        <div class="pl-2 flex items-center justify-between text-xs text-muted-foreground mt-4 border-t pt-3">
          <div class="flex items-center gap-1">
            <Clock class="w-3 h-3" />
            <span>{{ formatDate(props.album.createTime) }}</span>
          </div>
          
          <Button 
            variant="ghost" 
            size="icon" 
            class="h-6 w-6 text-muted-foreground hover:text-destructive hover:bg-destructive/10 -mr-1"
            @click="handleDelete"
          >
            <Trash2 class="w-3 h-3" />
          </Button>
        </div>
    </div>
  </div>
</template>
