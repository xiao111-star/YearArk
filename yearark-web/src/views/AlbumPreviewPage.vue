<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChevronLeft, ChevronRight, BookOpen, Layers } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import type { PageItem } from '@/components/BookViewer.vue'
import { previewAlbum, getAlbumDetail } from '@/api/album'

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

const pages = ref<PageItem[]>([])
const loading = ref(false)
const currentIndex = ref(0)
const albumName = ref('')

const isFirst = () => currentIndex.value === 0
const isLast = () => currentIndex.value >= pages.value.length - 1
const currentPage = () => pages.value[currentIndex.value]

function prev() {
  if (!isFirst()) currentIndex.value--
}

function next() {
  if (!isLast()) currentIndex.value++
}

function goToPage(idx: number) {
  currentIndex.value = idx
}

async function fetchData() {
  loading.value = true
  try {
    const [previewRes, detailRes] = await Promise.all([
      previewAlbum(albumId),
      getAlbumDetail(albumId),
    ])
    pages.value = previewRes.data?.data ?? []
    albumName.value = detailRes.data?.data?.name ?? '纪念册预览'
  } catch {
    pages.value = []
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push(`/album/${albumId}`)
}

onMounted(fetchData)
</script>

<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <Button variant="ghost" class="gap-2 pl-0 hover:bg-transparent hover:text-primary" @click="goBack">
        <ArrowLeft class="w-4 h-4" />
        返回详情
      </Button>
      <div class="flex items-center justify-between mt-2">
        <div>
          <h1 class="text-3xl font-serif font-bold tracking-tight text-primary">{{ albumName }}</h1>
          <p class="text-muted-foreground mt-1">
            <span v-if="!loading && pages.length > 0">共 {{ pages.length }} 页</span>
            <span v-else-if="!loading">暂无内容</span>
          </p>
        </div>
      </div>
    </div>

    <!-- Loading skeleton -->
    <div v-if="loading" class="space-y-4">
      <div class="h-[600px] rounded-xl bg-muted animate-pulse" />
      <div class="flex justify-center gap-2">
        <div class="h-9 w-24 rounded-md bg-muted animate-pulse" />
        <div class="h-9 w-32 rounded-md bg-muted animate-pulse" />
        <div class="h-9 w-24 rounded-md bg-muted animate-pulse" />
      </div>
    </div>

    <!-- Empty state -->
    <div
      v-else-if="pages.length === 0"
      class="flex flex-col items-center justify-center py-20 bg-card rounded-xl border border-dashed"
    >
      <div class="w-16 h-16 rounded-full bg-muted flex items-center justify-center mb-4">
        <BookOpen class="w-8 h-8 text-muted-foreground" />
      </div>
      <h3 class="text-lg font-medium">纪念册尚未生成</h3>
      <p class="text-muted-foreground mb-6">请先在详情页点击"生成纪念册"</p>
      <Button @click="goBack">返回详情页</Button>
    </div>

    <!-- Viewer -->
    <template v-else>
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6">
        <!-- Page thumbnails sidebar -->
        <div class="lg:col-span-1 order-2 lg:order-1">
          <Card>
            <CardContent class="p-3">
              <div class="flex items-center gap-2 mb-3 px-1">
                <Layers class="w-4 h-4 text-muted-foreground" />
                <span class="text-sm font-medium text-muted-foreground">页面导航</span>
              </div>
              <div class="space-y-1 max-h-[560px] overflow-y-auto pr-1">
                <button
                  v-for="(p, idx) in pages"
                  :key="p.pageId"
                  :class="[
                    'w-full text-left px-3 py-2 rounded-md text-sm transition-colors flex items-center gap-2',
                    idx === currentIndex
                      ? 'bg-primary text-primary-foreground'
                      : 'hover:bg-muted text-muted-foreground hover:text-foreground',
                  ]"
                  @click="goToPage(idx)"
                >
                  <span class="w-5 h-5 rounded-full flex items-center justify-center text-xs font-medium shrink-0"
                    :class="idx === currentIndex ? 'bg-primary-foreground/20' : 'bg-muted'"
                  >
                    {{ idx + 1 }}
                  </span>
                  第 {{ p.sort }} 页
                </button>
              </div>
            </CardContent>
          </Card>
        </div>

        <!-- Main viewer -->
        <div class="lg:col-span-3 order-1 lg:order-2 space-y-4">
          <Card class="overflow-hidden shadow-md">
            <CardContent class="p-0">
              <iframe
                v-if="currentPage()"
                :srcdoc="currentPage()!.html"
                class="w-full border-0"
                style="height: 600px"
                sandbox="allow-same-origin"
                title="纪念册页面"
              />
            </CardContent>
          </Card>

          <!-- Navigation controls -->
          <div class="flex items-center justify-between">
            <Button variant="outline" :disabled="isFirst()" @click="prev" class="gap-2">
              <ChevronLeft class="w-4 h-4" />
              上一页
            </Button>
            <span class="text-sm text-muted-foreground">
              第 {{ currentIndex + 1 }} 页 / 共 {{ pages.length }} 页
            </span>
            <Button variant="outline" :disabled="isLast()" @click="next" class="gap-2">
              下一页
              <ChevronRight class="w-4 h-4" />
            </Button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
