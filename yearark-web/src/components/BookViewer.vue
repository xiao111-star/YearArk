<script setup lang="ts">
import { ref, computed } from 'vue'
import { Button } from '@/components/ui/button'

export interface PageItem {
  pageId: number
  sort: number
  html: string
}

const props = defineProps<{
  pages: PageItem[]
}>()

const currentIndex = ref(0)

const totalPages = computed(() => props.pages.length)
const currentPage = computed(() => props.pages[currentIndex.value])
const isFirst = computed(() => currentIndex.value === 0)
const isLast = computed(() => currentIndex.value >= totalPages.value - 1)

function prev() {
  if (!isFirst.value) currentIndex.value--
}

function next() {
  if (!isLast.value) currentIndex.value++
}
</script>

<template>
  <div class="flex flex-col items-center gap-4">
    <!-- iframe 渲染区域 -->
    <div class="w-full max-w-4xl border rounded-lg overflow-hidden bg-white shadow-sm">
      <iframe
        v-if="currentPage"
        :srcdoc="currentPage.html"
        class="w-full border-0"
        style="height: 600px"
        sandbox="allow-same-origin"
        title="纪念册页面"
      />
    </div>

    <!-- 导航控制 -->
    <div class="flex items-center gap-4">
      <Button variant="outline" size="sm" :disabled="isFirst" @click="prev">
        上一页
      </Button>
      <span class="text-sm text-muted-foreground">
        第 {{ currentIndex + 1 }} 页 / 共 {{ totalPages }} 页
      </span>
      <Button variant="outline" size="sm" :disabled="isLast" @click="next">
        下一页
      </Button>
    </div>
  </div>
</template>
