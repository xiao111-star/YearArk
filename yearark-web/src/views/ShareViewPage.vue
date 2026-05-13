<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { BookOpen, ChevronLeft, ChevronRight, RotateCcw } from 'lucide-vue-next'
import FlipBook from '@/components/FlipBook.vue'
import type { FlipPage } from '@/components/FlipBook.vue'
import { getPublicAlbumView } from '@/api/public'
import { renderPage } from '@/utils/albumRenderer'

const route = useRoute()
const albumId = Number(route.params.id)

const pages = ref<FlipPage[]>([])
const loading = ref(true)
const error = ref(false)
const albumName = ref('')
const flipBookRef = ref<InstanceType<typeof FlipBook>>()

const isMobile = ref(false)
const isPortrait = ref(false)
const orientationLocked = ref(false)

/** 是否需要 CSS 强制横屏（移动端竖屏 + 未能锁定系统横屏时） */
const needsRotation = computed(() => isMobile.value && isPortrait.value && !orientationLocked.value)

/** 显示旋转提示（移动端竖屏时提示用户横屏） */
const showRotateHint = ref(false)

function checkDevice() {
  isMobile.value = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent) || window.innerWidth < 768
  isPortrait.value = window.innerHeight > window.innerWidth
}

// lock()/unlock() 是非标准 API，TS 的 DOM lib 未声明，这里做局部窄化类型断言
type LockableOrientation = ScreenOrientation & {
  lock?: (orientation: 'landscape' | 'portrait' | 'natural' | 'any') => Promise<void>
  unlock?: () => void
}

/** 尝试锁定屏幕方向为横屏 */
async function tryLockLandscape() {
  if (!isMobile.value) return
  try {
    const orientation = screen.orientation as LockableOrientation
    if (orientation?.lock) {
      await orientation.lock('landscape')
      orientationLocked.value = true
    }
  } catch {
    // 大多数浏览器不允许非全屏时锁定方向，降级到 CSS 方案
    orientationLocked.value = false
  }
}

/** 解锁屏幕方向 */
function unlockOrientation() {
  try {
    const orientation = screen.orientation as LockableOrientation
    if (orientationLocked.value && orientation?.unlock) {
      orientation.unlock()
    }
  } catch { /* ignore */ }
}

onMounted(() => {
  checkDevice()
  window.addEventListener('resize', checkDevice)
  window.addEventListener('orientationchange', () => {
    setTimeout(checkDevice, 200)
  })
  fetchData()
  tryLockLandscape()

  // 移动端竖屏时短暂显示提示
  if (isMobile.value && isPortrait.value) {
    showRotateHint.value = true
    setTimeout(() => { showRotateHint.value = false }, 4000)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', checkDevice)
  unlockOrientation()
})

// 当旋转状态变化时，重新初始化 FlipBook
watch(needsRotation, () => {
  nextTick(() => {
    // FlipBook 会通过 watch pages 重新初始化，这里触发一下
    const tmp = [...pages.value]
    pages.value = []
    nextTick(() => { pages.value = tmp })
  })
})

async function fetchData() {
  loading.value = true
  error.value = false
  try {
    const res = await getPublicAlbumView(albumId)
    const data = res.data?.data
    if (!data) { error.value = true; return }
    albumName.value = data.name ?? ''
    const rawPages = data.pages ?? []
    pages.value = rawPages.map((p: any) => ({
      pageId: p.pageId,
      sort: p.sort,
      html: p.templateHtml ? renderPage(p.templateHtml, p.data ?? {}) : (p.html ?? ''),
    }))
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="share-view-root" :class="{ 'force-landscape': needsRotation }">
    <!-- Loading -->
    <div v-if="loading" class="state-center">
      <div class="pulse-icon"><BookOpen class="w-10 h-10 text-gray-400" /></div>
      <p class="mt-4 text-sm text-gray-400">加载纪念册中...</p>
    </div>

    <!-- Error / Empty -->
    <div v-else-if="error || pages.length === 0" class="state-center">
      <div class="pulse-icon"><BookOpen class="w-10 h-10 text-gray-400" /></div>
      <p class="mt-4 text-sm text-gray-500">{{ error ? '纪念册不存在或已被删除' : '纪念册尚未生成' }}</p>
    </div>

    <!-- Content -->
    <template v-else>
      <div class="title-bar">
        <h1 class="title-text">{{ albumName }}</h1>
        <span class="page-indicator">
          {{ (flipBookRef?.currentPage ?? 0) + 1 }} / {{ flipBookRef?.totalPages ?? pages.length }}
        </span>
      </div>

      <div class="book-area">
        <FlipBook
          ref="flipBookRef"
          :pages="pages"
          :use-portrait="false"
          :max-width="1200"
        />
      </div>

      <div class="controls-bar">
        <button class="ctrl-btn" :disabled="flipBookRef?.currentPage === 0" @click="flipBookRef?.flipPrev()">
          <ChevronLeft class="w-5 h-5" />
        </button>
        <div class="dots">
          <div
            v-for="i in Math.min(flipBookRef?.totalPages ?? pages.length, 10)"
            :key="i"
            class="dot"
            :class="{ active: (flipBookRef?.currentPage ?? 0) === i - 1 }"
          />
          <span v-if="(flipBookRef?.totalPages ?? pages.length) > 10" class="text-xs text-gray-400 ml-1">
            ...{{ flipBookRef?.totalPages ?? pages.length }}页
          </span>
        </div>
        <button class="ctrl-btn" :disabled="flipBookRef?.currentPage === (flipBookRef?.totalPages ?? pages.length) - 1" @click="flipBookRef?.flipNext()">
          <ChevronRight class="w-5 h-5" />
        </button>
      </div>
    </template>

    <!-- 横屏提示 -->
    <div v-if="showRotateHint && isMobile && isPortrait" class="rotate-hint">
      <RotateCcw class="w-4 h-4 text-white/80 mr-1.5" />
      <span class="text-xs text-white/80">请横屏查看以获得最佳体验</span>
    </div>
  </div>
</template>

<style scoped>
.share-view-root {
  position: fixed;
  inset: 0;
  display: flex;
  flex-direction: column;
  background: #111;
  color: #fff;
  overflow: hidden;
  z-index: 9999;
}

/*
 * 移动端竖屏强制横屏：
 * 使用 transform rotate(90deg) 将整个容器旋转，
 * 宽高互换以适配旋转后的视口。
 * transform-origin 设为 top left，然后 left 偏移 100vw 使内容回到可视区域。
 */
.force-landscape {
  transform: rotate(90deg);
  transform-origin: top left;
  position: fixed;
  top: 0;
  left: 100vw;
  width: 100vh;
  height: 100vw;
}

.state-center {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.pulse-icon {
  width: 5rem;
  height: 5rem;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.5rem 1rem;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  flex-shrink: 0;
}

.title-text {
  font-size: 0.875rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.page-indicator {
  font-size: 0.75rem;
  color: rgba(255, 255, 255, 0.6);
  flex-shrink: 0;
  margin-left: 1rem;
}

.book-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem;
  overflow: hidden;
}

.controls-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 0.5rem 1rem;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  flex-shrink: 0;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: none;
  cursor: pointer;
  transition: background 0.2s;
}

.ctrl-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.2);
}

.ctrl-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

.dots {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 9999px;
  background: rgba(255, 255, 255, 0.2);
  transition: background 0.2s;
}

.dot.active {
  background: #fff;
}

.rotate-hint {
  position: fixed;
  bottom: 1rem;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.7);
  padding: 0.5rem 1rem;
  border-radius: 1rem;
  pointer-events: none;
  display: flex;
  align-items: center;
  animation: fadeOut 4s forwards;
}

.force-landscape .rotate-hint {
  display: none;
}

@keyframes fadeOut {
  0%, 70% { opacity: 1; }
  100% { opacity: 0; }
}
</style>
