<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChevronLeft, ChevronRight, BookOpen, Maximize2, Minimize2, Share2, Check, Copy } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import FlipBook from '@/components/FlipBook.vue'
import type { FlipPage } from '@/components/FlipBook.vue'
import { getAlbumEditData, getAlbumDetail } from '@/api/album'
import { renderPage } from '@/utils/albumRenderer'
import type { PageData } from '@/types/editor'

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

const pages = ref<FlipPage[]>([])
const loading = ref(false)
const albumName = ref('')
const albumDes = ref('')
const flipBookRef = ref<InstanceType<typeof FlipBook>>()
const isFullscreen = ref(false)
const showSharePopover = ref(false)
const copied = ref(false)

/** 生成分享链接：优先使用当前访问的 host（非 localhost 时直接用），否则回退到服务器 IP */
function getShareUrl(): string {
  const loc = window.location
  let host = loc.host
  // 如果是 localhost，替换为服务器实际 IP
  if (loc.hostname === 'localhost' || loc.hostname === '127.0.0.1') {
    host = '192.168.31.72:5173'
  }
  return `${loc.protocol}//${host}/view/${albumId}`
}

const shareUrl = getShareUrl()

function copyShareLink() {
  copyToClipboard(shareUrl).then((ok) => {
    if (ok) {
      copied.value = true
      setTimeout(() => { copied.value = false }, 2000)
    }
  })
}

/** 兼容非 HTTPS 环境的复制 */
function fallbackCopy(text: string): boolean {
  const ta = document.createElement('textarea')
  ta.value = text
  ta.style.cssText = 'position:fixed;left:-9999px;top:-9999px;opacity:0;'
  document.body.appendChild(ta)
  ta.select()
  let ok = false
  try { ok = document.execCommand('copy') } catch { /* ignore */ }
  document.body.removeChild(ta)
  return ok
}

async function copyToClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      return true
    }
  } catch { /* Clipboard API 不可用 */ }
  return fallbackCopy(text)
}

function toggleSharePopover() {
  showSharePopover.value = !showSharePopover.value
  copied.value = false
}

/** 点击外部关闭分享弹窗 */
function onClickOutside(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (showSharePopover.value && !target.closest('.share-popover-wrapper')) {
    showSharePopover.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onClickOutside)
})

async function fetchData() {
  loading.value = true
  try {
    const [editDataRes, detailRes] = await Promise.all([
      getAlbumEditData(albumId),
      getAlbumDetail(albumId),
    ])
    const editPages: PageData[] = editDataRes.data?.data ?? []
    pages.value = editPages.map((p) => ({
      pageId: p.pageId,
      sort: p.sort,
      html: renderPage(p.templateHtml, p.data),
    }))
    albumName.value = detailRes.data?.data?.name ?? '纪念册预览'
    albumDes.value = detailRes.data?.data?.des ?? ''
  } catch {
    pages.value = []
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push(`/album/${albumId}`)
}

function toggleFullscreen() {
  isFullscreen.value = !isFullscreen.value
}

onMounted(fetchData)
</script>

<template>
  <div :class="isFullscreen ? 'fixed inset-0 z-50 bg-background' : ''">
    <!-- Top bar -->
    <div class="border-b bg-card/80 backdrop-blur" :class="isFullscreen ? 'px-6' : ''">
      <div :class="isFullscreen ? '' : 'container mx-auto max-w-7xl'" class="flex items-center justify-between h-14 px-2">
        <div class="flex items-center gap-3 min-w-0">
          <Button variant="ghost" size="sm" class="gap-1.5 shrink-0 text-muted-foreground hover:text-primary" @click="goBack">
            <ArrowLeft class="w-3.5 h-3.5" />
            返回
          </Button>
          <div class="h-5 w-px bg-border" />
          <div class="min-w-0">
            <h1 class="text-sm font-serif font-bold text-foreground truncate">{{ albumName }}</h1>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <span v-if="!loading && pages.length > 0" class="text-xs text-muted-foreground hidden sm:inline">
            第 {{ (flipBookRef?.currentPage ?? 0) + 1 }} / {{ flipBookRef?.totalPages ?? pages.length }} 页
          </span>
          <div class="relative share-popover-wrapper">
            <Button variant="outline" size="sm" class="gap-1.5" @click.stop="toggleSharePopover">
              <Share2 class="w-3.5 h-3.5" />
              <span class="hidden sm:inline">分享</span>
            </Button>
            <!-- Share popover -->
            <Teleport to="body">
              <div
                v-if="showSharePopover"
                class="fixed inset-0 z-[10000]"
                style="pointer-events: none;"
              >
                <div
                  ref="sharePopoverEl"
                  class="share-popover-wrapper absolute w-80 rounded-lg border bg-card p-4 shadow-lg"
                  style="pointer-events: auto; top: 3.5rem; right: 1rem;"
                >
                  <p class="text-sm font-medium mb-2">分享链接</p>
                  <p class="text-xs text-muted-foreground mb-3">复制链接发送给朋友，无需登录即可查看</p>
                  <div class="flex gap-2">
                    <input
                      :value="shareUrl"
                      readonly
                      class="flex-1 h-9 rounded-md border bg-muted px-3 text-xs text-foreground select-all outline-none"
                      @focus="($event.target as HTMLInputElement).select()"
                    />
                    <Button size="sm" class="gap-1.5 shrink-0" @click="copyShareLink">
                      <Check v-if="copied" class="w-3.5 h-3.5" />
                      <Copy v-else class="w-3.5 h-3.5" />
                      {{ copied ? '已复制' : '复制' }}
                    </Button>
                  </div>
                </div>
              </div>
            </Teleport>
          </div>
          <Button variant="ghost" size="icon" class="w-8 h-8" @click="toggleFullscreen">
            <Minimize2 v-if="isFullscreen" class="w-4 h-4" />
            <Maximize2 v-else class="w-4 h-4" />
          </Button>
        </div>
      </div>
    </div>

    <!-- Content area -->
    <div class="flex flex-col items-center justify-center" :class="isFullscreen ? 'h-[calc(100vh-3.5rem-3.5rem)]' : 'py-8'">
      <!-- Loading -->
      <div v-if="loading" class="flex flex-col items-center gap-4 py-20">
        <div class="w-16 h-16 rounded-full bg-muted flex items-center justify-center animate-pulse">
          <BookOpen class="w-8 h-8 text-muted-foreground" />
        </div>
        <p class="text-sm text-muted-foreground">加载纪念册中...</p>
      </div>

      <!-- Empty state -->
      <div
        v-else-if="pages.length === 0"
        class="flex flex-col items-center justify-center py-20"
      >
        <div class="w-20 h-20 rounded-full bg-muted flex items-center justify-center mb-5">
          <BookOpen class="w-10 h-10 text-muted-foreground" />
        </div>
        <h3 class="text-lg font-serif font-medium mb-2">纪念册尚未生成</h3>
        <p class="text-sm text-muted-foreground mb-6">请先在详情页点击"生成纪念册"</p>
        <Button @click="goBack" class="gap-2">
          <ArrowLeft class="w-4 h-4" />
          返回详情页
        </Button>
      </div>

      <!-- FlipBook -->
      <template v-else>
        <div class="w-full max-w-5xl px-4">
          <div class="rounded-xl bg-card border shadow-sm p-4 md:p-8">
            <FlipBook ref="flipBookRef" :pages="pages" />
          </div>
        </div>
      </template>
    </div>

    <!-- Bottom controls -->
    <div v-if="!loading && pages.length > 0" class="border-t bg-card/80 backdrop-blur">
      <div class="flex items-center justify-center gap-4 h-14 px-4">
        <Button
          variant="outline"
          size="sm"
          :disabled="flipBookRef?.currentPage === 0"
          @click="flipBookRef?.flipPrev()"
          class="gap-1.5"
        >
          <ChevronLeft class="w-4 h-4" />
          <span class="hidden sm:inline">上一页</span>
        </Button>

        <!-- Page dots / indicator -->
        <div class="flex items-center gap-1.5">
          <div
            v-for="i in Math.min(flipBookRef?.totalPages ?? pages.length, 12)"
            :key="i"
            class="w-2 h-2 rounded-full transition-colors"
            :class="(flipBookRef?.currentPage ?? 0) === i - 1 ? 'bg-primary' : 'bg-border'"
          />
          <span v-if="(flipBookRef?.totalPages ?? pages.length) > 12" class="text-xs text-muted-foreground ml-1">
            ...共 {{ flipBookRef?.totalPages ?? pages.length }} 页
          </span>
        </div>

        <Button
          variant="outline"
          size="sm"
          :disabled="flipBookRef?.currentPage === (flipBookRef?.totalPages ?? pages.length) - 1"
          @click="flipBookRef?.flipNext()"
          class="gap-1.5"
        >
          <span class="hidden sm:inline">下一页</span>
          <ChevronRight class="w-4 h-4" />
        </Button>
      </div>
    </div>
  </div>
</template>
