<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import AlbumCard from '@/components/AlbumCard.vue'
import type { Album } from '@/components/AlbumCard.vue'
import { listAlbums, deleteAlbum } from '@/api/album'

const router = useRouter()
const albums = ref<Album[]>([])
const loading = ref(false)

async function fetchAlbums() {
  loading.value = true
  try {
    const res = await listAlbums()
    albums.value = res.data?.data ?? []
  } catch {
    albums.value = []
  } finally {
    loading.value = false
  }
}

function goCreate() {
  router.push('/album/create')
}

function goDetail(id: number) {
  router.push(`/album/${id}`)
}

async function handleDelete(id: number) {
  const confirmed = window.confirm('确定要删除这本纪念册吗？删除后无法恢复。')
  if (!confirmed) return
  try {
    await deleteAlbum(id)
    albums.value = albums.value.filter((a) => a.id !== id)
  } catch {
    // 错误由 axios 拦截器统一处理
  }
}

onMounted(fetchAlbums)
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-bold">我的纪念册</h1>
      <Button @click="goCreate">创建纪念册</Button>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="text-center py-12 text-muted-foreground">加载中...</div>

    <!-- 空状态 -->
    <div v-else-if="albums.length === 0" class="text-center py-12">
      <p class="text-muted-foreground mb-4">还没有纪念册，快来创建一本吧</p>
      <Button @click="goCreate">创建纪念册</Button>
    </div>

    <!-- 纪念册卡片网格 -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      <AlbumCard
        v-for="album in albums"
        :key="album.id"
        :album="album"
        @detail="goDetail"
        @delete="handleDelete"
      />
    </div>
  </div>
</template>
