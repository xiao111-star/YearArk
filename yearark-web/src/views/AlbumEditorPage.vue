<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, BookOpen } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useAlbumEditor } from '@/composables/useAlbumEditor'
import { useAutoSave } from '@/composables/useAutoSave'
import { renderPage, isImageSlotValue } from '@/utils/albumRenderer'
import { clampImageValue } from '@/utils/imageClamp'

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

// ---- Image drag state ----
const draggingImage = ref(false)
let dragStartX = 0
let dragStartY = 0
let dragStartFocusX = 0
let dragStartFocusY = 0

// ---- Page refs for click detection ----
const pageRefs = ref<Record<number, HTMLElement>>({})

function setPageRef(pageId: number, el: HTMLElement | null) {
  if (el) pageRefs.value[pageId] = el
}

// ---- Computed ----
const activePage = computed(() =>
  pages.value.find(p => p.pageId === activePageId.value) ?? null
)

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
        selectImageSlot(pageId, slot.id, target as HTMLImageElement)
        return
      }
    }
  }

  // Check if clicked inside a text node area
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

// ---- Active image element tracking ----
let activeImgEl: HTMLImageElement | null = null

function selectImageSlot(pageId: number, slotId: string, imgEl: HTMLImageElement) {
  // Clear previous selection styling
  clearImageHighlight()
  clearTextHighlight()

  activePageId.value = pageId
  activeSlotId.value = slotId
  editMode.value = 'image'
  activeImgEl = imgEl

  // Add a subtle highlight to the selected image
  imgEl.style.outline = '2px solid hsl(200 80% 60%)'
  imgEl.style.outlineOffset = '-2px'
  imgEl.style.cursor = 'grab'
}

function selectTextSlot(pageId: number, slotId: string, targetEl: HTMLElement) {
  clearImageHighlight()
  clearTextHighlight()

  activePageId.value = pageId
  activeSlotId.value = slotId
  editMode.value = 'text'

  // Make the element contenteditable with a soft blue outline
  nextTick(() => {
    targetEl.setAttribute('contenteditable', 'true')
    targetEl.style.outline = '2px solid hsl(200 80% 60%)'
    targetEl.style.outlineOffset = '2px'
    targetEl.style.borderRadius = '4px'
    targetEl.style.cursor = 'text'
    activeTextEl = targetEl
    targetEl.focus()

    const onBlur = () => {
      const newText = targetEl.textContent || ''
      cleanupTextEl(targetEl)
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

let activeTextEl: HTMLElement | null = null

function cleanupTextEl(el: HTMLElement) {
  el.removeAttribute('contenteditable')
  el.style.outline = ''
  el.style.outlineOffset = ''
  el.style.borderRadius = ''
  el.style.cursor = ''
}

function clearTextHighlight() {
  if (activeTextEl) {
    cleanupTextEl(activeTextEl)
    activeTextEl = null
  }
}

function clearImageHighlight() {
  if (activeImgEl) {
    activeImgEl.style.outline = ''
    activeImgEl.style.outlineOffset = ''
    activeImgEl.style.cursor = ''
    activeImgEl = null
  }
}

function deselect() {
  clearImageHighlight()
  clearTextHighlight()
  editMode.value = 'idle'
  activePageId.value = null
  activeSlotId.value = null
  draggingImage.value = false
}

// ---- Image drag to adjust focus point ----
function onImagePointerDown(e: PointerEvent) {
  if (editMode.value !== 'image' || !activeImgEl || !activePageId.value || !activeSlotId.value) return

  // Only respond to clicks on the active image
  const target = e.target as HTMLElement
  if (target !== activeImgEl && !activeImgEl.contains(target)) return

  e.preventDefault()
  const container = activeImgEl.parentElement
  if (!container) return

  container.setPointerCapture(e.pointerId)
  draggingImage.value = true
  activeImgEl.style.cursor = 'grabbing'

  dragStartX = e.clientX
  dragStartY = e.clientY

  const val = activeImageValue.value
  if (val) {
    dragStartFocusX = val.focus_x
    dragStartFocusY = val.focus_y
  }
}

function onImagePointerMove(e: PointerEvent) {
  if (!draggingImage.value || !activeImgEl || !activePageId.value || !activeSlotId.value) return

  const container = activeImgEl.parentElement
  if (!container) return

  const rect = container.getBoundingClientRect()
  // Convert pixel delta to focus delta (inverted: drag right → focus moves left)
  const dx = -(e.clientX - dragStartX) / rect.width
  const dy = -(e.clientY - dragStartY) / rect.height

  const page = pages.value.find(p => p.pageId === activePageId.value)
  if (!page) return
  const val = page.data[activeSlotId.value!]
  if (!isImageSlotValue(val)) return

  const clamped = clampImageValue({
    focus_x: dragStartFocusX + dx,
    focus_y: dragStartFocusY + dy,
    scale: val.scale,
  })
  updateSlotValue(activePageId.value, activeSlotId.value!, {
    url: val.url,
    focus_x: clamped.focus_x,
    focus_y: clamped.focus_y,
    scale: clamped.scale,
  })
}

function onImagePointerUp() {
  if (draggingImage.value && activeImgEl) {
    activeImgEl.style.cursor = 'grab'
  }
  draggingImage.value = false
}

// ---- Mouse wheel to zoom image ----
function onImageWheel(e: WheelEvent) {
  if (editMode.value !== 'image' || !activePageId.value || !activeSlotId.value) return

  // Only respond when hovering over the active image
  const target = e.target as HTMLElement
  if (target !== activeImgEl && !activeImgEl?.contains(target)) return

  e.preventDefault()
  e.stopPropagation()

  const page = pages.value.find(p => p.pageId === activePageId.value)
  if (!page) return
  const val = page.data[activeSlotId.value!]
  if (!isImageSlotValue(val)) return

  // Scroll up = zoom in, scroll down = zoom out
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  const clamped = clampImageValue({
    focus_x: val.focus_x,
    focus_y: val.focus_y,
    scale: val.scale + delta,
  })
  updateSlotValue(activePageId.value, activeSlotId.value!, {
    url: val.url,
    focus_x: clamped.focus_x,
    focus_y: clamped.focus_y,
    scale: clamped.scale,
  })
}

// ---- Content size detection & scaling ----
const contentW = ref(0)
const contentH = ref(0)
const pageScale = ref(1)
const pageAreaRef = ref<HTMLElement | null>(null)

function detectContentSize() {
  if (pages.value.length === 0) return
  const html = getRenderedHtml(pages.value[0])
  if (!html) return
  const probe = document.createElement('div')
  probe.style.cssText = 'position:absolute;left:-9999px;top:-9999px;visibility:hidden;display:inline-block;'
  probe.innerHTML = html
  document.body.appendChild(probe)
  contentW.value = probe.scrollWidth || 794
  contentH.value = probe.scrollHeight || 1123
  document.body.removeChild(probe)
}

function updatePageScale() {
  if (!pageAreaRef.value || contentW.value <= 0) return
  const available = pageAreaRef.value.clientWidth - 32
  pageScale.value = Math.min(1, available / contentW.value)
}

let scaleObserver: ResizeObserver | null = null

function goBack() {
  router.push(`/album/${albumId.value}`)
}

function handleOutsideClick(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (editMode.value !== 'idle' && !target.closest('.editor-pages')) {
    deselect()
  }
}

onMounted(async () => {
  await loadEditData()
  detectContentSize()
  await nextTick()
  if (pageAreaRef.value) {
    updatePageScale()
    scaleObserver = new ResizeObserver(updatePageScale)
    scaleObserver.observe(pageAreaRef.value)
  }
})

onUnmounted(() => {
  scaleObserver?.disconnect()
})
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
          <span v-if="editMode === 'image'" class="text-xs text-muted-foreground">
            拖拽调整位置 · 滚轮缩放
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

      <!-- Editor layout (full width, no side panel) -->
      <div v-else class="h-[calc(100vh-3.5rem)] overflow-y-auto editor-pages" ref="pageAreaRef">
        <div class="py-6 flex flex-col items-center gap-6">
          <div
            v-for="page in pages"
            :key="page.pageId"
            class="page-scale-wrapper"
            :style="{ width: contentW * pageScale + 'px', height: contentH * pageScale + 'px' }"
          >
            <div
              :ref="(el) => setPageRef(page.pageId, el as HTMLElement)"
              class="page-card rounded-xl bg-card border shadow-sm overflow-hidden cursor-pointer"
              :style="{ width: contentW + 'px', height: contentH + 'px', transform: `scale(${pageScale})`, transformOrigin: 'top left' }"
              @click.stop="handlePageClick(page.pageId, $event)"
              @pointerdown.stop="onImagePointerDown($event)"
              @pointermove="onImagePointerMove"
              @pointerup="onImagePointerUp"
              @pointercancel="onImagePointerUp"
              @wheel.stop="onImageWheel($event)"
            >
              <div v-html="getRenderedHtml(page)" class="page-content" />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-scale-wrapper {
  position: relative;
  flex-shrink: 0;
}

.page-card {
  position: absolute;
  top: 0;
  left: 0;
}

.page-content {
  width: 100%;
  height: 100%;
  overflow: hidden;
}
</style>
