<script setup lang="ts">
import { BookOpen, Clock, Trash2, Share2, Home, Check, Copy } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'

export interface Album {
  id: number
  name: string
  des: string
  status: number // 0=草稿, 1=已发布
  isPublic: number // 0=不公开, 1=公开到首页
  createAt: string
  coverUrl?: string
}

const props = defineProps<{ album: Album }>()
const emit = defineEmits<{ 
  detail: [id: number]
  delete: [id: number]
  togglePublic: [id: number]
  publish: [id: number]
  share: [id: number]
}>()

const showSharePopover = ref(false)
const copied = ref(false)
const shareButtonRef = ref<HTMLElement>()

// 计算弹窗位置
const popoverStyle = computed(() => {
  if (!shareButtonRef.value) return {}
  
  const rect = shareButtonRef.value.getBoundingClientRect()
  const popoverWidth = 288 // w-72 = 18rem = 288px
  
  // 计算弹窗位置：按钮右对齐，下方 4px
  let left = rect.right - popoverWidth
  const top = rect.bottom + 4
  
  // 防止弹窗超出屏幕左侧
  if (left < 8) {
    left = 8
  }
  
  // 防止弹窗超出屏幕右侧
  if (left + popoverWidth > window.innerWidth - 8) {
    left = window.innerWidth - popoverWidth - 8
  }
  
  return {
    left: `${left}px`,
    top: `${top}px`,
  }
})

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function handleDelete(e: Event) {
  e.stopPropagation()
  emit('delete', props.album.id)
}

function handleTogglePublic(e: Event) {
  e.stopPropagation()
  emit('togglePublic', props.album.id)
}

function handlePublish(e: Event) {
  e.stopPropagation()
  emit('publish', props.album.id)
}

async function handleShare(e: Event) {
  e.stopPropagation()
  console.log('handleShare clicked', shareButtonRef.value)
  showSharePopover.value = !showSharePopover.value
  copied.value = false
  
  // 等待 DOM 更新后重新计算位置
  if (showSharePopover.value) {
    await nextTick()
    console.log('popover shown, style:', popoverStyle.value)
  }
}

function getShareUrl(): string {
  const loc = window.location
  let host = loc.host
  if (loc.hostname === 'localhost' || loc.hostname === '127.0.0.1') {
    host = 'yearark.top'
  }
  return `${loc.protocol}//${host}/view/${props.album.id}`
}

async function copyShareLink() {
  const url = getShareUrl()
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url)
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
      return
    }
  } catch { /* ignore */ }
  
  // Fallback
  const ta = document.createElement('textarea')
  ta.value = url
  ta.style.cssText = 'position:fixed;left:-9999px;top:-9999px;opacity:0;'
  document.body.appendChild(ta)
  ta.select()
  try {
    document.execCommand('copy')
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  } catch { /* ignore */ }
  document.body.removeChild(ta)
}

// 点击外部关闭弹窗
function handleClickOutside(event: MouseEvent) {
  if (!showSharePopover.value) return
  
  const target = event.target as HTMLElement
  const popoverContainer = target.closest('.share-popover-container')
  
  if (!popoverContainer) {
    showSharePopover.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div
    class="group relative flex flex-col h-[320px] bg-card rounded-xl border shadow-sm hover:shadow-md transition-all overflow-hidden cursor-pointer"
    @click="emit('detail', props.album.id)"
  >
    <div class="h-48 bg-muted relative overflow-hidden">
      <img
        v-if="props.album.coverUrl"
        :src="props.album.coverUrl"
        :alt="props.album.name"
        class="absolute inset-0 w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
      />
      <div
        v-if="props.album.coverUrl"
        class="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent pointer-events-none"
      />
      <div
        v-if="!props.album.coverUrl"
        class="absolute inset-0 bg-gradient-to-br from-primary/5 to-primary/20"
      />
      <div
        v-if="!props.album.coverUrl"
        class="absolute inset-0 flex items-center justify-center"
      >
        <BookOpen class="w-12 h-12 text-primary/20" />
      </div>
      <!-- 标签容器 -->
      <div class="absolute top-2.5 left-2.5 flex flex-wrap gap-1.5 max-w-[calc(100%-5rem)]">
        <!-- 首页标签 -->
        <span
          v-if="props.album.isPublic === 1"
          class="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-medium rounded-full bg-blue-500/90 text-white backdrop-blur-sm hover:bg-blue-600/90 transition-colors"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="w-2.5 h-2.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M11.47 3.84a.75.75 0 011.06 0l8.69 8.69a.75.75 0 101.06-1.06l-8.689-8.69a2.25 2.25 0 00-3.182 0l-8.69 8.69a.75.75 0 001.061 1.06l8.69-8.69z" />
            <path d="M12 5.432l8.159 8.159c.03.03.06.058.091.086v6.198c0 1.035-.84 1.875-1.875 1.875H15a.75.75 0 01-.75-.75v-4.5a.75.75 0 00-.75-.75h-3a.75.75 0 00-.75.75V21a.75.75 0 01-.75.75H5.625a1.875 1.875 0 01-1.875-1.875v-6.198a2.29 2.29 0 00.091-.086L12 5.43z" />
          </svg>
          首页
        </span>
      </div>
      
      <!-- 状态标签 -->
      <div class="absolute top-2.5 right-2.5">
        <span
          class="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-medium rounded-full backdrop-blur-sm transition-colors"
          :class="props.album.status === 1 
            ? 'bg-teal-600/85 text-white hover:bg-teal-700/85' 
            : 'bg-gray-700/75 text-white hover:bg-gray-800/75'"
        >
          <svg v-if="props.album.status === 1" xmlns="http://www.w3.org/2000/svg" class="w-2.5 h-2.5" viewBox="0 0 24 24" fill="currentColor">
            <path fill-rule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12zm13.36-1.814a.75.75 0 10-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 00-1.06 1.06l2.25 2.25a.75.75 0 001.14-.094l3.75-5.25z" clip-rule="evenodd" />
          </svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" class="w-2.5 h-2.5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M21.731 2.269a2.625 2.625 0 00-3.712 0l-1.157 1.157 3.712 3.712 1.157-1.157a2.625 2.625 0 000-3.712zM19.513 8.199l-3.712-3.712-8.4 8.4a5.25 5.25 0 00-1.32 2.214l-.8 2.685a.75.75 0 00.933.933l2.685-.8a5.25 5.25 0 002.214-1.32l8.4-8.4z" />
            <path d="M5.25 5.25a3 3 0 00-3 3v10.5a3 3 0 003 3h10.5a3 3 0 003-3V13.5a.75.75 0 00-1.5 0v5.25a1.5 1.5 0 01-1.5 1.5H5.25a1.5 1.5 0 01-1.5-1.5V8.25a1.5 1.5 0 011.5-1.5h5.25a.75.75 0 000-1.5H5.25z" />
          </svg>
          {{ props.album.status === 1 ? '已发布' : '草稿' }}
        </span>
      </div>
    </div>

    <div class="flex-1 p-4 flex flex-col justify-between bg-card relative">
      <div class="absolute left-0 top-0 bottom-0 w-1 bg-primary/10 group-hover:bg-primary transition-colors" />
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
          <span>{{ formatDate(props.album.createAt) }}</span>
        </div>
        <div class="flex items-center gap-1">
          <!-- 发布按钮 - 只有草稿状态才显示 -->
          <Button
            v-if="props.album.status === 0"
            variant="ghost"
            size="icon"
            class="h-6 w-6 text-muted-foreground hover:text-emerald-600 hover:bg-emerald-50"
            @click="handlePublish"
            title="发布纪念册"
          >
            <svg xmlns="http://www.w3.org/2000/svg" class="w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 5v14M5 12l7 7 7-7"/>
            </svg>
          </Button>
          <!-- 放到首页按钮 - 只有发布状态才显示 -->
          <Button
            v-if="props.album.status === 1"
            variant="ghost"
            size="icon"
            class="h-6 w-6 text-muted-foreground hover:text-primary hover:bg-primary/10"
            :class="props.album.isPublic === 1 ? 'text-blue-600' : ''"
            @click="handleTogglePublic"
            :title="props.album.isPublic === 1 ? '取消放到首页' : '放到首页'"
          >
            <Home class="w-3 h-3" />
          </Button>
          <!-- 分享按钮 - 只有发布状态才显示 -->
          <div v-if="props.album.status === 1" class="share-popover-container">
            <button
              ref="shareButtonRef"
              class="inline-flex items-center justify-center h-6 w-6 rounded-md text-muted-foreground hover:text-primary hover:bg-primary/10 transition-colors"
              @click="handleShare"
              title="分享"
              type="button"
            >
              <Share2 class="w-3 h-3" />
            </button>
          </div>
          
          <!-- Share popover - 使用 Teleport 避免被 overflow-hidden 裁剪 -->
          <Teleport to="body">
            <div
              v-if="showSharePopover && shareButtonRef"
              class="fixed w-72 rounded-lg border bg-card p-3 shadow-lg z-[100] animate-in fade-in slide-in-from-top-2 duration-200"
              :style="popoverStyle"
              @click.stop
            >
              <p class="text-xs font-medium mb-1.5">分享链接</p>
              <p class="text-[11px] text-muted-foreground mb-2">复制链接发送给朋友，无需登录即可查看</p>
              <div class="flex gap-2">
                <input
                  :value="getShareUrl()"
                  readonly
                  class="flex-1 h-8 rounded-md border bg-muted px-2.5 text-xs text-foreground select-all outline-none focus:ring-1 focus:ring-primary"
                  @focus="($event.target as HTMLInputElement).select()"
                />
                <Button size="sm" class="gap-1.5 shrink-0 h-8 text-xs" @click="copyShareLink">
                  <Check v-if="copied" class="w-3 h-3" />
                  <Copy v-else class="w-3 h-3" />
                  {{ copied ? '已复制' : '复制' }}
                </Button>
              </div>
            </div>
          </Teleport>
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
  </div>
</template>
