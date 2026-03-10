<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ChevronLeft, ChevronRight, BookOpen } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import FlipBook from '@/components/FlipBook.vue'
import type { FlipPage } from '@/components/FlipBook.vue'
import { previewAlbum, getAlbumDetail } from '@/api/album'

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

const pages = ref<FlipPage[]>([])
const loading = ref(false)
const albumName = ref('')
const flipBookRef = ref<InstanceType<typeof FlipBook>>()

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
            <span v-if="!loading && pages.length > 0">共 {{ pages.length }} 页 · 拖拽或点击边角翻页</span>
            <span v-else-if="!loading">暂无内容</span>
          </p>
        </div>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="space-y-4">
      <div class="h-[600px] rounded-xl bg-muted animate-pulse" />
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

    <!-- FlipBook Viewer -->
    <template v-else>
      <div class="flex flex-col items-center gap-4">
        <div class="w-full rounded-xl bg-card border shadow-sm p-6">
          <FlipBook ref="flipBookRef" :pages="pages" />
        </div>

        <!-- Navigation controls -->
        <div class="flex items-center gap-6">
          <Button
            variant="outline"
            size="sm"
            :disabled="flipBookRef?.currentPage === 0"
            @click="flipBookRef?.flipPrev()"
            class="gap-2"
          >
            <ChevronLeft class="w-4 h-4" />
            上一页
          </Button>
          <span class="text-sm text-muted-foreground">
            第 {{ (flipBookRef?.currentPage ?? 0) + 1 }} 页 / 共 {{ flipBookRef?.totalPages ?? pages.length }} 页
          </span>
          <Button
            variant="outline"
            size="sm"
            :disabled="flipBookRef?.currentPage === (flipBookRef?.totalPages ?? pages.length) - 1"
            @click="flipBookRef?.flipNext()"
            class="gap-2"
          >
            下一页
            <ChevronRight class="w-4 h-4" />
          </Button>
        </div>
      </div>
    </template>
  </div>
</template>
