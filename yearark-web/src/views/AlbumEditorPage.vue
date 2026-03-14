<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, BookOpen, X } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useAlbumEditor } from '@/composables/useAlbumEditor'
import { useAutoSave } from '@/composables/useAutoSave'
import { renderPage, isImageSlotValue } from '@/utils/albumRenderer'
import { clampImageValue } from '@/utils/imageClamp'

import { listMedia } from '@/api/media'
import type { ImageSlotValue, SlotDef } from '@/types/editor'

const route = useRoute()
const router = useRouter()
const albumId = computed(() => Number(route.params.id))

const { pages, loading, albumName, loadEditData, updateSlotValue } = useAlbumEditor(albumId)
const { saving } = useAutoSave()

// ---- Editor state ----
type EditMode = 'idle' | 'text' | 'image'
const editMode = ref<EditMode>('idle')
const activePageId = ref<number | null>(null)
const activeSlotId = ref<string | null>(null)

// ---- Media library ----
const allMedia = ref<{ id: number; content: string }[]>([])
const mediaLoading = ref(false)

// ---- Image adjust (inline on the preview) ----
const draggingFocus = ref(false)

// ---- Page refs for click detection ----
const pageRefs = ref<Record<number, HTMLElement>>({})

function setPageRef(pageId: number, el: HTMLElement | null) {
  if (el) pageRefs.value[pageId] = el
}

// ---- Computed ----
const activePage = computed(() =>
  pages.value.find(p => p.pageId === activePageId.value) ?? null
)

const activeSlotDef = computed<SlotDef | null>(() => {
  if (!activePage.value || !activeSlotId.value) return null
  try {
    const schema = JSON.parse(activePage.value.schemaContent || '{}')
    return (schema.slots || []).find((s: SlotDef) => s.id === activeSlotId.value) ?? null
  } catch { return null }
})

const activeImageValue = computed<ImageSlotValue | null>(() => {
  if (!activePage.value || !activeSlotId.value) return null
  const val = activePage.value.data[activeSlotId.value]
  if (isImageSlotValue(val)) return val
  return null
})

// ---- Rendered HTML per page ----
function getRenderedHtml(page: typeof pages.value[0]) {
  return renderPage(page.templateHtml, page.data)
}

// ---- Slot detection via click on rendered DOM ----
function getSlotDefs(page: typeof pages.value[0]): SlotDef[] {
  try {
    const schema = JSON.parse(page.schemaContent || '{}')
    return schema.slots || []
  } catch { return [] }
}

function handlePageClick(pageId: number, e: MouseEvent) {
  const target = e.target as HTMLElement
  const page = pages.value.find(p => p.pageId === pageId)
  if (!page) return

  const slotDefs = getSlotDefs(page)

  // Check if clicked on an <img> that corresponds to an image slot
  if (target.tagName === 'IMG') {
    const imgSrc = (target as HTMLImageElement).src
    for (const slot of slotDefs) {
      if (slot.type !== 'image') continue
      const val = page.data[slot.id]
      if (isImageSlotValue(val) && val.url && imgSrc.includes(val.url)) {
        selectImageSlot(pageId, slot.id)
        return
      }
    }
  }

  // Check if clicked inside a text node area — find closest text slot
  // We match by checking if the text content of the clicked element matches a text slot value
  const clickedText = target.textContent?.trim()
  if (clickedText) {
    for (const slot of slotDefs) {
      if (slot.type !== 'text') continue
      const val = page.data[slot.id]
      if (typeof val === 'string' && val.trim() === clickedText) {
        selectTextSlot(pageId, slot.id, target)
        return
      }
    }
  }

  // Clicked on empty area — deselect
  deselect()
}

function selectImageSlot(pageId: number, slotId: string) {
  activePageId.value = pageId
  activeSlotId.value = slotId
  editMode.value = 'image'
  loadAllMedia()
}

function selectTextSlot(pageId: number, slotId: string, targetEl: HTMLElement) {
  activePageId.value = pageId
  activeSlotId.value = slotId
  editMode.value = 'text'

  // Make the element contenteditable
  nextTick(() => {
    targetEl.setAttribute('contenteditable', 'true')
    targetEl.style.outline = '2px solid hsl(var(--primary))'
    targetEl.style.outlineOffset = '2px'
    targetEl.style.borderRadius = '4px'
    targetEl.focus()

    const onBlur = () => {
      const newText = targetEl.textContent || ''
      targetEl.removeAttribute('contenteditable')
      targetEl.style.outline = ''
      targetEl.style.outlineOffset = ''
      targetEl.style.borderRadius = ''
      targetEl.removeEventListener('blur', onBlur)
      targetEl.removeEventListener('keydown', onKeydown)

      const page = pages.value.find(p => p.pageId === pageId)
      if (page) {
        const oldVal = page.data[slotId]
        if (typeof oldVal === 'string' && oldVal !== newText) {
          updateSlotValue(pageId, slotId, newText)
        }
      }
      deselect()
    }

    const onKeydown = (ev: KeyboardEvent) => {
      if (ev.key === 'Enter') {
        ev.preventDefault()
        targetEl.blur()
      }
      if (ev.key === 'Escape') {
        // Restore original text
        const page = pages.value.find(p => p.pageId === pageId)
        if (page) {
          const val = page.data[slotId]
          targetEl.textContent = typeof val === 'string' ? val : ''
        }
        targetEl.blur()
      }
    }

    targetEl.addEventListener('blur', onBlur)
    targetEl.addEventListener('keydown', onKeydown)
  })
}

function deselect() {
  editMode.value = 'idle'
  activePageId.value = null
  activeSlotId.value = null
}

// ---- Media library ----
async function loadAllMedia() {
  mediaLoading.value = true
  try {
    const res = await listMedia({ albumId: albumId.value })
    const items = (res.data?.data ?? []) as any[]
    allMedia.value = items
      .filter((m: any) => m.type === 2) // images only
      .map((m: any) => ({ id: m.id, content: m.content }))
  } catch {
    allMedia.value = []
  } finally {
    mediaLoading.value = false
  }
}

function pickMedia(media: { id: number; content: string }) {
  if (!activePageId.value || !activeSlotId.value) return
  const newValue: ImageSlotValue = {
    url: media.content,
    focus_x: 0.5,
    focus_y: 0.5,
    scale: 1.0,
  }
  updateSlotValue(activePageId.value, activeSlotId.value, newValue)
}

// ---- Focus drag on the active image (directly on the page preview) ----
function onImagePointerDown(e: PointerEvent) {
  if (editMode.value !== 'image' || !activePageId.value || !activeSlotId.value) return
  const imgEl = findActiveImgElement()
  if (!imgEl) return

  const container = imgEl.parentElement
  if (!container) return

  container.setPointerCapture(e.pointerId)
  draggingFocus.value = true
  updateFocusFromPointer(e, container)
}

function onImagePointerMove(e: PointerEvent) {
  if (!draggingFocus.value) return
  const imgEl = findActiveImgElement()
  if (!imgEl?.parentElement) return
  updateFocusFromPointer(e, imgEl.parentElement)
}

function onImagePointerUp() {
  draggingFocus.value = false
}

function updateFocusFromPointer(e: PointerEvent, container: HTMLElement) {
  if (!activePageId.value || !activeSlotId.value) return
  const rect = container.getBoundingClientRect()
  const x = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
  const y = Math.max(0, Math.min(1, (e.clientY - rect.top) / rect.height))

  const page = pages.value.find(p => p.pageId === activePageId.value)
  if (!page) return
  const val = page.data[activeSlotId.value!]
  if (!isImageSlotValue(val)) return

  const clamped = clampImageValue({ focus_x: x, focus_y: y, scale: val.scale })
  updateSlotValue(activePageId.value, activeSlotId.value!, {
    url: val.url,
    focus_x: clamped.focus_x,
    focus_y: clamped.focus_y,
    scale: clamped.scale,
  })
}

function onScaleInput(e: Event) {
  if (!activePageId.value || !activeSlotId.value) return
  const page = pages.value.find(p => p.pageId === activePageId.value)
  if (!page) return
  const val = page.data[activeSlotId.value!]
  if (!isImageSlotValue(val)) return

  const raw = parseFloat((e.target as HTMLInputElement).value)
  const clamped = clampImageValue({ focus_x: val.focus_x, focus_y: val.focus_y, scale: raw })
  updateSlotValue(activePageId.value, activeSlotId.value!, {
    url: val.url,
    focus_x: clamped.focus_x,
    focus_y: clamped.focus_y,
    scale: clamped.scale,
  })
}

function findActiveImgElement(): HTMLImageElement | null {
  if (!activePageId.value || !activeSlotId.value) return null
  const page = pages.value.find(p => p.pageId === activePageId.value)
  if (!page) return null
  const val = page.data[activeSlotId.value!]
  if (!isImageSlotValue(val) || !val.url) return null

  const container = pageRefs.value[activePageId.value]
  if (!container) return null

  const imgs = container.querySelectorAll('img')
  for (const img of imgs) {
    if (img.src.includes(val.url)) return img
  }
  return null
}

function goBack() {
  router.push(`/album/${albumId.value}`)
}

// Close panel on outside click
function handleOutsideClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  // If clicking outside the page content and media panel, deselect
  if (editMode.value !== 'idle' && !target.closest('.editor-pages') && !target.closest('.media-panel')) {
    deselect()
  }
}

onMounted(loadEditData)
</script>

<template>
  <div class="min-h-screen flex flex-col" @click="handleOutsideClick">
    <!-- Top navigation bar -->
    <div class="sticky top-0 z-30 border-b bg-card/80 backdrop-blur">
      <div class="container mx-auto max-w-7xl flex items-center justify-between h-14 px-2">
        <div class="flex items-center gap-3 min-w-0">
          <Button variant="ghost" size="sm" class="gap-1.5 shrink-0 text-muted-foreground hover:text-primary" @click="goBack">
            <ArrowLeft class="w-3.5 h-3.5" />
            返回
          </Button>
          <div class="h-5 w-px bg-border" />
          <h1 class="text-sm font-serif font-bold text-foreground truncate">{{ albumName || '编辑纪念册' }}</h1>
        </div>
        <div class="flex items-center gap-2">
          <span v-if="saving" class="flex items-center gap-1.5 text-xs text-muted-foreground">
            <Loader2 class="w-3.5 h-3.5 animate-spin" />
            保存中...
          </span>
        </div>
      </div>
    </div>

    <!-- Main content area -->
    <div class="flex-1">
      <!-- Loading -->
      <div v-if="loading" class="flex flex-col items-center gap-4 py-20">
        <Loader2 class="w-8 h-8 text-muted-foreground animate-spin" />
        <p class="text-sm text-muted-foreground">加载编辑数据中...</p>
      </div>

      <!-- Empty -->
      <div v-else-if="pages.length === 0" class="flex flex-col items-center justify-center py-20">
        <BookOpen class="w-10 h-10 text-muted-foreground mb-4" />
        <h3 class="text-lg font-serif font-medium mb-2">纪念册尚未生成</h3>
        <p class="text-sm text-muted-foreground mb-6">请先在详情页点击"生成纪念册"</p>
        <Button @click="goBack" class="gap-2"><ArrowLeft class="w-4 h-4" />返回详情页</Button>
      </div>

      <!-- Editor layout -->
      <div v-else class="flex h-[calc(100vh-3.5rem)]">
        <!-- Left: Page preview area -->
        <div
          class="editor-pages flex-1 overflow-y-auto transition-all duration-300"
          :class="editMode === 'image' ? 'pr-0' : ''"
        >
          <div
            class="py-6 px-4 space-y-6 transition-all duration-300 mx-auto"
            :class="editMode === 'image' ? 'max-w-3xl' : 'max-w-4xl'"
          >
            <div
              v-for="page in pages"
              :key="page.pageId"
              :ref="(el) => setPageRef(page.pageId, el as HTMLElement)"
              class="rounded-xl bg-card border shadow-sm overflow-hidden cursor-pointer"
              :class="{
                'ring-2 ring-primary': activePageId === page.pageId,
              }"
              @click.stop="handlePageClick(page.pageId, $event)"
              @pointerdown="activePageId === page.pageId && editMode === 'image' ? onImagePointerDown($event) : undefined"
              @pointermove="onImagePointerMove"
              @pointerup="onImagePointerUp"
              @pointercancel="onImagePointerUp"
            >
              <div v-html="getRenderedHtml(page)" class="page-content" />
            </div>
          </div>
        </div>

        <!-- Right: Media panel (only when image slot is selected) -->
        <div
          v-if="editMode === 'image'"
          class="media-panel w-80 border-l bg-card flex flex-col shrink-0 overflow-hidden"
          @click.stop
        >
          <!-- Panel header -->
          <div class="flex items-center justify-between px-4 py-3 border-b">
            <h3 class="text-sm font-medium">素材库</h3>
            <button class="text-muted-foreground hover:text-foreground" @click="deselect">
              <X class="w-4 h-4" />
            </button>
          </div>

          <!-- Scale slider -->
          <div v-if="activeImageValue" class="px-4 py-3 border-b">
            <div class="flex items-center gap-2">
              <span class="text-xs text-muted-foreground shrink-0">缩放</span>
              <input
                type="range" min="0.5" max="3.0" step="0.1"
                :value="activeImageValue.scale"
                class="flex-1 h-1.5 bg-muted rounded-lg appearance-none cursor-pointer accent-primary"
                @input="onScaleInput"
              />
              <span class="text-xs text-muted-foreground w-8 text-right tabular-nums">
                {{ activeImageValue.scale.toFixed(1) }}
              </span>
            </div>
            <p class="text-xs text-muted-foreground mt-1">拖拽图片调整焦点</p>
          </div>

          <!-- Media grid -->
          <div class="flex-1 overflow-y-auto px-4 py-3">
            <div v-if="mediaLoading" class="flex items-center justify-center py-8">
              <Loader2 class="w-5 h-5 animate-spin text-muted-foreground" />
            </div>
            <div v-else-if="allMedia.length === 0" class="text-center py-8 text-sm text-muted-foreground">
              暂无图片素材
            </div>
            <div v-else class="grid grid-cols-2 gap-2">
              <div
                v-for="item in allMedia"
                :key="item.id"
                class="aspect-square rounded-lg overflow-hidden border cursor-pointer hover:ring-2 hover:ring-primary transition-shadow"
                @click="pickMedia(item)"
              >
                <img :src="item.content" :alt="`素材 ${item.id}`" class="w-full h-full object-cover" loading="lazy" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
