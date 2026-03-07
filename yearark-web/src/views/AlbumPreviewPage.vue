<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import BookViewer from '@/components/BookViewer.vue'
import type { PageItem } from '@/components/BookViewer.vue'
import { previewAlbum } from '@/api/album'

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

const pages = ref<PageItem[]>([])
const loading = ref(false)

async function fetchPages() {
  loading.value = true
  try {
    const res = await previewAlbum(albumId)
    pages.value = res.data?.data ?? []
  } catch {
    pages.value = []
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push(`/album/${albumId}`)
}

onMounted(fetchPages)
</script>

<template>
  <div>
    <!-- 返回按钮 -->
    <div class="mb-4">
      <Button variant="ghost" size="sm" @click="goBack">← 返回详情</Button>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="text-center py-12 text-muted-foreground">加载中...</div>

    <!-- 未生成提示 -->
    <div v-else-if="pages.length === 0" class="text-center py-20">
      <p class="text-muted-foreground mb-4">纪念册尚未生成，请先点击生成</p>
      <Button @click="goBack">返回详情页</Button>
    </div>

    <!-- 预览 -->
    <BookViewer v-else :pages="pages" />
  </div>
</template>
