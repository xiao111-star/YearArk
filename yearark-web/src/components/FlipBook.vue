<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { PageFlip } from 'page-flip'

export interface FlipPage {
  pageId: number
  sort: number
  html: string
  /** 原始内容宽度（px），用于计算缩放比 */
  contentWidth?: number
  /** 原始内容高度（px），用于计算缩放比 */
  contentHeight?: number
}

const props = withDefaults(
  defineProps<{
    pages: FlipPage[]
    width?: number
    height?: number
    showCover?: boolean
    /** 纪念册 HTML 内容的原始设计宽度 */
    contentWidth?: number
    /** 纪念册 HTML 内容的原始设计高度 */
    contentHeight?: number
    /**
     * 是否允许自动切换到单页（portrait）模式。
     * true = 容器窄时自动单页，false = 始终双页展开。
     * 默认 true（预览页），分享页传 false 强制双页。
     */
    usePortrait?: boolean
    /** 单页最大宽度，双页模式下实际占用 2x */
    maxWidth?: number
  }>(),
  {
    width: 550,
    height: 733,
    showCover: true,
    contentWidth: 0,
    contentHeight: 0,
    usePortrait: true,
    maxWidth: 800,
  },
)

const emit = defineEmits<{
  (e: 'flip', page: number): void
}>()

const bookRef = ref<HTMLElement>()
let pageFlip: PageFlip | null = null
const currentPage = ref(0)
const totalPages = ref(0)

/**
 * 探测第一页 HTML 内容的原始尺寸
 * 通过离屏渲染获取真实宽高
 */
function detectContentSize(): { w: number; h: number } {
  if (props.contentWidth > 0 && props.contentHeight > 0) {
    return { w: props.contentWidth, h: props.contentHeight }
  }
  // 离屏探测
  const probe = document.createElement('div')
  probe.style.cssText = 'position:absolute;left:-9999px;top:-9999px;visibility:hidden;display:inline-block;'
  probe.innerHTML = props.pages[0]?.html ?? ''
  document.body.appendChild(probe)
  const w = probe.scrollWidth || props.width
  const h = probe.scrollHeight || props.height
  document.body.removeChild(probe)
  return { w, h }
}

function initFlipBook() {
  if (!bookRef.value || props.pages.length === 0) return
  destroyFlipBook()

  nextTick(() => {
    const { w: cw, h: ch } = detectContentSize()

    // 把内容原始宽高比传给 page-flip，保证翻页区域比例与内容一致
    const aspectRatio = cw / ch
    const pageW = props.width
    const pageH = Math.round(pageW / aspectRatio)

    pageFlip = new PageFlip(bookRef.value!, {
      width: pageW,
      height: pageH,
      size: 'stretch',
      minWidth: 300,
      maxWidth: props.maxWidth,
      minHeight: Math.round(300 / aspectRatio),
      maxHeight: Math.round(props.maxWidth / aspectRatio),
      showCover: props.showCover,
      maxShadowOpacity: 0.5,
      mobileScrollSupport: true,
      flippingTime: 800,
      usePortrait: props.usePortrait,
      startZIndex: 0,
      autoSize: true,
      drawShadow: true,
      clickEventForward: true,
      swipeDistance: 30,
    })

    // 设置 CSS 变量供 page-content 缩放使用
    bookRef.value!.style.setProperty('--content-width', `${cw}px`)
    bookRef.value!.style.setProperty('--content-height', `${ch}px`)

    const pageElements = bookRef.value!.querySelectorAll('.flip-page')
    if (pageElements.length > 0) {
      pageFlip.loadFromHTML(pageElements as NodeListOf<HTMLElement>)
    }

    pageFlip.on('flip', (e) => {
      currentPage.value = e.data as number
      emit('flip', e.data as number)
    })

    totalPages.value = pageFlip.getPageCount()

    // 设置缩放监听，确保内容完整展示
    setupScaleObserver()
  })
}

function destroyFlipBook() {
  if (pageFlip) {
    pageFlip.destroy()
    pageFlip = null
  }
  if (scaleObserver) {
    scaleObserver.disconnect()
    scaleObserver = null
  }
}

let scaleObserver: MutationObserver | null = null

/**
 * page-flip 运行时会动态设置 .flip-page 的 inline width/height，
 * 监听变化后计算 scale 并应用到 .page-content 上
 */
function setupScaleObserver() {
  if (!bookRef.value) return
  const { w: cw, h: ch } = detectContentSize()
  if (cw <= 0 || ch <= 0) return

  const applyScale = () => {
    const pageEls = bookRef.value?.querySelectorAll('.flip-page')
    if (!pageEls) return
    pageEls.forEach((el) => {
      const htmlEl = el as HTMLElement
      // page-flip 通过 inline style 设置的实际渲染宽高
      const renderedW = htmlEl.offsetWidth || props.width
      const renderedH = htmlEl.offsetHeight || props.height
      const scaleX = renderedW / cw
      const scaleY = renderedH / ch
      const scale = Math.min(scaleX, scaleY)
      const content = htmlEl.querySelector('.page-content') as HTMLElement
      if (content) {
        content.style.transform = `scale(${scale})`
      }
    })
  }

  // 初始应用一次
  setTimeout(applyScale, 100)

  // 监听 page-flip 对 DOM 的修改（翻页时会重新设置 inline style）
  scaleObserver = new MutationObserver(applyScale)
  scaleObserver.observe(bookRef.value, {
    attributes: true,
    subtree: true,
    attributeFilter: ['style'],
  })
}

function flipPrev() {
  pageFlip?.flipPrev('top')
}

function flipNext() {
  pageFlip?.flipNext('bottom')
}

watch(
  () => props.pages,
  () => {
    nextTick(() => initFlipBook())
  },
)

onMounted(() => {
  nextTick(() => initFlipBook())
})

onBeforeUnmount(() => {
  destroyFlipBook()
})

defineExpose({ flipPrev, flipNext, currentPage, totalPages })
</script>

<template>
  <div class="flipbook-wrapper">
    <div ref="bookRef" class="flipbook-container">
      <div
        v-for="(page, idx) in pages"
        :key="page.pageId"
        class="flip-page"
        :data-density="idx === 0 || idx === pages.length - 1 ? 'hard' : 'soft'"
      >
        <div class="page-content" v-html="page.html" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.flipbook-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
}

.flipbook-container {
  position: relative;
}

.flip-page {
  background-color: #fff;
  overflow: hidden;
}

/*
 * 关键：page-flip 会给 .flip-page 设置固定宽高，
 * 但内容 HTML 的原始尺寸可能远大于此。
 * 通过 transform: scale 将内容缩放到翻页区域内完整展示。
 *
 * page-flip 运行时会把实际页面宽高写到 .flip-page 的 inline style 上，
 * 我们用 CSS 变量 --content-width / --content-height 记录原始内容尺寸，
 * 然后 page-content 以原始尺寸渲染，再 scale 缩小到容器内。
 */
.page-content {
  width: var(--content-width, 100%);
  height: var(--content-height, 100%);
  transform-origin: 0 0;
  /* scale 由 JS 在 initFlipBook 后通过 ResizeObserver 动态设置 */
  overflow: hidden;
}

/* page-flip 库生成的内部样式覆盖 */
:deep(.stf__parent) {
  margin: 0 auto;
}

:deep(.stf__wrapper) {
  margin: 0 auto !important;
}
</style>
