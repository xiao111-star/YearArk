<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loader2, BookOpen, ImagePlus, ChevronRight, Layers } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { useAlbumEditor } from '@/composables/useAlbumEditor'
import { useAutoSave } from '@/composables/useAutoSave'
import { renderPage, isImageSlotValue } from '@/utils/albumRenderer'
import { clampImageValue } from '@/utils/imageClamp'
import MediaSidePanel from '@/components/editor/MediaSidePanel.vue'

import type { ImageSlotValue, SlotDef } from '@/types/editor'

const route = useRoute()
const router = useRouter()
const albumId = computed(() => Number(route.params.id))

const { pages, loading, albumName, loadEditData, updateSlotValue, savePageData } = useAlbumEditor(albumId)
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
// Store the temporary adjusted values during drag/zoom
const pendingImageValue = ref<{ focus_x: number; focus_y: number; scale: number } | null>(null)

// ---- Media Picker state ----
function onReplaceMediaPick(media: { id: number; url: string }) {
  if (activePageId.value && activeSlotId.value) {
    const page = pages.value.find(p => p.pageId === activePageId.value)
    if (page) {
      const val = page.data[activeSlotId.value]
      if (isImageSlotValue(val)) {
        updateSlotValue(activePageId.value, activeSlotId.value, {
          ...val,
          url: media.url,
        }, true)
      }
    }
  }
}

// ---- Page refs for click detection ----
const pageRefs = ref<Record<number, HTMLElement>>({})
const thumbRefs = ref<Record<number, HTMLElement>>({})
const thumbnailListRef = ref<HTMLElement | null>(null)

function setPageRef(pageId: number, el: HTMLElement | null) {
  if (el) pageRefs.value[pageId] = el
}

function setThumbRef(pageId: number, el: HTMLElement | null) {
  if (el) thumbRefs.value[pageId] = el
}

// ---- Computed ----
const activePage = computed(() =>
  pages.value.find(p => p.pageId === activePageId.value) ?? null
)

// Select a page from thumbnail list
function selectPage(pageId: number) {
  activePageId.value = pageId
  deselect() // Clear slot selection when switching pages
  
  // Scroll to page in canvas
  const el = pageRefs.value[pageId]
  if (el && pageAreaRef.value) {
    // Adding a small delay to ensure DOM is ready
    nextTick(() => {
      // Use scrollIntoView to scroll the main canvas to the selected page
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    })
  }
}

// Track currently visible page during scroll
const visiblePageId = ref<number | null>(null)

function onCanvasScroll() {
  if (!pageAreaRef.value) return
  
  const container = pageAreaRef.value
  const containerCenter = container.scrollTop + container.clientHeight / 2

  let closestPageId: number | null = null
  let minDistance = Infinity

  // Find the page closest to the center of the viewport
  for (const page of pages.value) {
    const el = pageRefs.value[page.pageId]
    if (!el) continue

    // Since pages are within a scaling wrapper, we need to calculate relative to container
    // el.offsetTop gets the offset relative to the closest positioned ancestor.
    // In our layout, the wrapper is in the flex column. We can use getBoundingClientRect 
    // or offsetTop from the container. getBoundingClientRect is safer.
    const rect = el.getBoundingClientRect()
    const containerRect = container.getBoundingClientRect()
    
    const elCenterY = (rect.top - containerRect.top) + rect.height / 2
    
    // We want the element whose center is closest to the container's center (which is containerRect.height / 2)
    const distance = Math.abs(elCenterY - containerRect.height / 2)
    
    if (distance < minDistance) {
      minDistance = distance
      closestPageId = page.pageId
    }
  }

  if (closestPageId !== null && visiblePageId.value !== closestPageId) {
    visiblePageId.value = closestPageId
    syncThumbnailScroll(closestPageId)
  }
}

function syncThumbnailScroll(pageId: number) {
  if (!thumbnailListRef.value) return
  
  const thumbEl = thumbRefs.value[pageId]
  if (!thumbEl) return

  // Smoothly scroll the thumbnail into view if it's out of bounds
  thumbEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
}

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
    const imgSrc = decodeURIComponent((target as HTMLImageElement).src)
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

function preventDefault(e: Event) {
  e.preventDefault()
}

function selectImageSlot(pageId: number, slotId: string, imgEl: HTMLImageElement) {
  // Clear previous selection styling
  clearImageHighlight()
  clearTextHighlight()

  activePageId.value = pageId
  activeSlotId.value = slotId
  editMode.value = 'image'
  activeImgEl = imgEl

  // Disable context menu on the active image
  activeImgEl.addEventListener('contextmenu', preventDefault)

  // Add a clear highlight to the selected image
  imgEl.style.outline = '3px solid hsl(210, 100%, 50%)'
  imgEl.style.outlineOffset = '-3px'
  imgEl.style.boxShadow = '0 0 0 4px rgba(255, 255, 255, 0.5), 0 8px 16px rgba(0,0,0,0.2)'
  imgEl.style.cursor = 'grab'
  imgEl.style.zIndex = '10' // Bring to front
  imgEl.style.transition = 'box-shadow 0.2s ease, outline 0.2s ease'
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
    targetEl.style.outline = '2px dashed hsl(210, 100%, 50%)'
    targetEl.style.outlineOffset = '4px'
    targetEl.style.backgroundColor = 'rgba(255, 255, 255, 0.9)'
    targetEl.style.boxShadow = '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)'
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
          updateSlotValue(pageId, slotId, newText, true)
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
  el.style.backgroundColor = ''
  el.style.boxShadow = ''
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
    activeImgEl.removeEventListener('contextmenu', preventDefault)
    activeImgEl.style.outline = ''
    activeImgEl.style.outlineOffset = ''
    activeImgEl.style.boxShadow = ''
    activeImgEl.style.zIndex = ''
    activeImgEl.style.transition = ''
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

  e.preventDefault()
  const container = activeImgEl.parentElement
  if (!container) return

  const page = pages.value.find(p => p.pageId === activePageId.value)
  if (!page) return
  const val = page.data[activeSlotId.value!]
  if (!isImageSlotValue(val)) return

  const rect = container.getBoundingClientRect()
  
  // Calculate movement sensitivity based on actual image dimensions
  // To make drag 1:1, we need to know how many pixels of the image are hidden.
  // object-position % is based on the *difference* between the image's displayed size and the container size.
  // For example, if image is 200px wide and container is 100px wide, 100% object-position means moving 100px.
  // So 1% = 1px. If we move mouse 1px, we should change focus by 1%.
  // If image is 150px wide, 100% object-position means moving 50px.
  // So 1% = 0.5px. If we move mouse 1px, we should change focus by 2%.
  // Therefore: delta_focus = delta_pixel / (image_displayed_size - container_size)
  
  // Let's get the natural dimensions of the active image
  const imgNaturalW = activeImgEl.naturalWidth
  const imgNaturalH = activeImgEl.naturalHeight
  const containerW = rect.width
  const containerH = rect.height
  
  const currentScale = pendingImageValue.value?.scale ?? val.scale
  
  // Calculate displayed size of the image before object-position is applied (object-fit: cover)
  const imgRatio = imgNaturalW / imgNaturalH
  const containerRatio = containerW / containerH
  
  let displayedW, displayedH
  if (imgRatio > containerRatio) {
    // Image is wider than container, height is constrained to container height
    displayedH = containerH * currentScale
    displayedW = (containerH * imgRatio) * currentScale
  } else {
    // Image is taller than container, width is constrained to container width
    displayedW = containerW * currentScale
    displayedH = (containerW / imgRatio) * currentScale
  }
  
  // The draggable range in pixels
  const rangeX = Math.max(0, displayedW - containerW)
  const rangeY = Math.max(0, displayedH - containerH)
  
  // If range is 0, the image fits exactly, so it shouldn't move.
  // If range > 0, calculate delta based on range to achieve 1:1 pixel movement.
  const dx = rangeX > 0 ? -(e.clientX - dragStartX) / rangeX : 0
  const dy = rangeY > 0 ? -(e.clientY - dragStartY) / rangeY : 0

  const clamped = clampImageValue({
    focus_x: Number((dragStartFocusX + dx).toFixed(4)), // Use more precision for internal math
    focus_y: Number((dragStartFocusY + dy).toFixed(4)),
    scale: val.scale,
  })

  // Store for pointer up
  pendingImageValue.value = clamped

  // Direct DOM manipulation for smooth preview
  const fx = (clamped.focus_x * 100).toFixed(1)
  const fy = (clamped.focus_y * 100).toFixed(1)
  
  activeImgEl.style.objectPosition = `${fx}% ${fy}%`
  activeImgEl.style.transformOrigin = `${fx}% ${fy}%`
}

function onImagePointerUp() {
  if (draggingImage.value && activeImgEl && activePageId.value && activeSlotId.value) {
    activeImgEl.style.cursor = 'grab'
    
    // Save the final value if we have one
    if (pendingImageValue.value) {
      const page = pages.value.find(p => p.pageId === activePageId.value)
      if (page) {
        const val = page.data[activeSlotId.value!]
        if (isImageSlotValue(val)) {
           updateSlotValue(activePageId.value!, activeSlotId.value!, {
             ...val,
             focus_x: pendingImageValue.value.focus_x,
             focus_y: pendingImageValue.value.focus_y,
             scale: pendingImageValue.value.scale
           }, true)
        }
      }
      pendingImageValue.value = null
    }
  }
  draggingImage.value = false
}

// ---- Mouse wheel to zoom image ----
let wheelSaveTimer: number | null = null

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

  // Use pending value if exists (during rapid scrolling), otherwise use current stored value
  const currentScale = pendingImageValue.value?.scale ?? val.scale
  const currentFocusX = pendingImageValue.value?.focus_x ?? val.focus_x
  const currentFocusY = pendingImageValue.value?.focus_y ?? val.focus_y
  
  // Reduce zoom speed (was 0.1, now 0.05)
  // Reduced further to 0.02 as requested
  const delta = e.deltaY > 0 ? -0.02 : 0.02
  
  const clamped = clampImageValue({
    focus_x: currentFocusX,
    focus_y: currentFocusY,
    scale: currentScale + delta,
  })

  // Store temporary value
  pendingImageValue.value = clamped

  // Direct DOM manipulation
  activeImgEl.style.transform = `scale(${clamped.scale})`

  if (wheelSaveTimer) clearTimeout(wheelSaveTimer)
  wheelSaveTimer = window.setTimeout(() => {
    if (activePageId.value && activeSlotId.value && pendingImageValue.value) {
      updateSlotValue(activePageId.value, activeSlotId.value, {
        ...val,
        scale: pendingImageValue.value.scale,
        focus_x: pendingImageValue.value.focus_x,
        focus_y: pendingImageValue.value.focus_y
      }, true)
      pendingImageValue.value = null
    }
  }, 500)
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
  // Add more padding for the canvas area
  const available = pageAreaRef.value.clientWidth - 80
  pageScale.value = Math.min(1.5, available / contentW.value) // allow slight upscale if needed, or keep 1
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

function preventDefaultBehavior(e: Event) {
  e.preventDefault()
}

// Disable browser zoom via Ctrl+Wheel
function preventBrowserZoom(e: WheelEvent) {
  if (e.ctrlKey || e.metaKey) {
    e.preventDefault()
  }
}

onMounted(async () => {
  // Global event listeners to prevent default behaviors
  document.addEventListener('contextmenu', preventDefaultBehavior)
  document.addEventListener('dragstart', preventDefaultBehavior)
  document.addEventListener('wheel', preventBrowserZoom, { passive: false })

  await loadEditData()
  detectContentSize()
  await nextTick()
  if (pageAreaRef.value) {
    updatePageScale()
    scaleObserver = new ResizeObserver(updatePageScale)
    scaleObserver.observe(pageAreaRef.value)
    
    // Initialize visible page
    onCanvasScroll()
  }
})

onUnmounted(() => {
  // Clean up global listeners
  document.removeEventListener('contextmenu', preventDefaultBehavior)
  document.removeEventListener('dragstart', preventDefaultBehavior)
  document.removeEventListener('wheel', preventBrowserZoom)

  scaleObserver?.disconnect()
})
</script>

<template>
  <div class="h-screen flex flex-col overflow-hidden bg-background" @click="handleOutsideClick">
    <!-- Top navigation bar (Toolbar) -->
    <div class="h-14 border-b bg-card flex items-center justify-between px-4 shrink-0 z-30 shadow-sm">
      <div class="flex items-center gap-4">
        <Button variant="ghost" size="sm" class="gap-1.5 text-muted-foreground hover:text-foreground" @click="goBack">
          <ArrowLeft class="w-4 h-4" />
          返回
        </Button>
        <div class="h-4 w-px bg-border" />
        <div class="flex items-center gap-2">
          <BookOpen class="w-4 h-4 text-primary" />
          <h1 class="text-sm font-medium text-foreground truncate max-w-[200px]">{{ albumName || '未命名纪念册' }}</h1>
        </div>
      </div>
      
      <!-- Center Toolbar -->
      <div class="flex-1 flex justify-center items-center gap-2">
         <span v-if="editMode === 'image'" class="inline-flex items-center px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-medium">
          <ImagePlus class="w-3.5 h-3.5 mr-1.5" />
          图片编辑模式：拖拽移动 · 滚轮缩放
        </span>
        <span v-else-if="editMode === 'text'" class="inline-flex items-center px-3 py-1 rounded-full bg-primary/10 text-primary text-xs font-medium">
          文字编辑模式：直接输入修改
        </span>
      </div>

      <!-- Right Toolbar -->
      <div class="flex items-center gap-3 w-[200px] justify-end">
        <span v-if="saving" class="flex items-center gap-1.5 text-xs text-muted-foreground bg-muted px-2 py-1 rounded-md">
          <Loader2 class="w-3.5 h-3.5 animate-spin" />
          保存中
        </span>
        <span v-else class="text-xs text-muted-foreground">已自动保存</span>
        <Button size="sm" class="gap-1.5 ml-2" @click="router.push(`/album/${albumId}/preview`)">
          <BookOpen class="w-3.5 h-3.5" />
          预览
        </Button>
      </div>
    </div>

    <!-- Main content area -->
    <div class="flex-1 flex overflow-hidden">
      <!-- Loading -->
      <div v-if="loading" class="flex-1 flex flex-col items-center justify-center gap-4">
        <Loader2 class="w-8 h-8 text-muted-foreground animate-spin" />
        <p class="text-sm text-muted-foreground">加载编辑数据中...</p>
      </div>

      <!-- Empty -->
      <div v-else-if="pages.length === 0" class="flex-1 flex flex-col items-center justify-center">
        <BookOpen class="w-12 h-12 text-muted-foreground/50 mb-4" />
        <h3 class="text-lg font-medium mb-2">纪念册尚未生成</h3>
        <p class="text-sm text-muted-foreground mb-6">请先在详情页点击"生成纪念册"</p>
        <Button @click="goBack" class="gap-2"><ArrowLeft class="w-4 h-4" />返回详情页</Button>
      </div>

      <!-- Editor layout (Figma style) -->
      <template v-else>
        <!-- Left Panel: Page Thumbnails -->
        <div class="w-64 border-r bg-card flex flex-col shrink-0 z-20 shadow-[1px_0_10px_rgba(0,0,0,0.02)]">
          <div class="px-4 py-3 border-b bg-muted/20 flex items-center gap-2">
            <Layers class="w-4 h-4 text-muted-foreground" />
            <span class="text-sm font-medium">页面列表</span>
            <span class="text-xs text-muted-foreground ml-auto">{{ pages.length }} 页</span>
          </div>
          <div class="flex-1 overflow-y-auto p-3 space-y-3 custom-scrollbar" ref="thumbnailListRef">
            <div
              v-for="(page, idx) in pages"
              :key="page.pageId"
              :ref="(el) => setThumbRef(page.pageId, el as HTMLElement)"
              class="group relative rounded-md border-2 transition-all cursor-pointer overflow-hidden bg-background"
              :class="(activePageId === page.pageId || visiblePageId === page.pageId) ? 'border-primary ring-2 ring-primary/20' : 'border-transparent hover:border-muted-foreground/30'"
              @click="selectPage(page.pageId)"
            >
              <div class="aspect-[1/1.414] w-full bg-muted/10 relative">
                <!-- Thumbnail rendering: scaled down via CSS transform -->
                <div 
                  class="absolute top-0 left-0 origin-top-left pointer-events-none"
                  :style="{
                    width: contentW + 'px', 
                    height: contentH + 'px', 
                    transform: `scale(${220 / contentW})` 
                  }"
                >
                  <div v-html="getRenderedHtml(page)" class="w-full h-full overflow-hidden" />
                </div>
              </div>
              <div class="absolute bottom-0 inset-x-0 bg-gradient-to-t from-black/50 to-transparent p-2">
                <span class="text-white text-xs font-medium drop-shadow-md">第 {{ idx + 1 }} 页</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Center Panel: Canvas -->
        <div 
          class="flex-1 overflow-y-auto bg-muted/30 editor-pages relative" 
          ref="pageAreaRef"
          @scroll="onCanvasScroll"
        >
          <div class="py-12 flex flex-col items-center gap-12 min-h-full">
            <div
              v-for="page in pages"
              :key="page.pageId"
              class="page-scale-wrapper"
              :style="{ width: contentW * pageScale + 'px', height: contentH * pageScale + 'px' }"
            >
              <div
                :ref="(el) => setPageRef(page.pageId, el as HTMLElement)"
                class="page-card rounded-sm bg-background shadow-xl overflow-hidden cursor-pointer transition-shadow"
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

        <!-- Right Panel: Properties / Media Picker (Fixed width placeholder to prevent layout shift) -->
        <div class="w-80 border-l bg-card flex flex-col shrink-0 z-20 shadow-[-1px_0_10px_rgba(0,0,0,0.02)]">
          <MediaSidePanel 
            v-if="editMode === 'image'"
            :album-id="albumId" 
            @pick="onReplaceMediaPick" 
          />
          <div v-else class="flex-1 flex flex-col items-center justify-center text-muted-foreground p-6 text-center">
            <ImagePlus class="w-12 h-12 mb-4 opacity-20" />
            <p class="text-sm font-medium mb-1">未选中图片</p>
            <p class="text-xs opacity-70">点击画布中的图片即可在此替换</p>
          </div>
        </div>
      </template>
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

/* Custom scrollbar for thumbnail list */
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

:deep(img) {
  -webkit-user-drag: none;
  user-select: none;
  touch-action: none;
}
</style>
