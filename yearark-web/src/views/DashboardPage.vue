<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search, BookOpen, Clock, Trash2, AlertTriangle } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Card, CardContent } from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import AlbumCard, { type Album } from '@/components/AlbumCard.vue'
import { listAlbums, deleteAlbum } from '@/api/album'

const router = useRouter()
const albums = ref<Album[]>([])
const loading = ref(false)
const searchQuery = ref('')

// Delete Dialog State
const deleteDialogOpen = ref(false)
const albumToDelete = ref<number | null>(null)
const deleting = ref(false)

const filteredAlbums = computed(() => {
  if (!searchQuery.value) return albums.value
  const q = searchQuery.value.toLowerCase()
  return albums.value.filter(a => 
    a.name.toLowerCase().includes(q) || 
    (a.des && a.des.toLowerCase().includes(q))
  )
})

async function fetchAlbums() {
  loading.value = true
  try {
    const res = await listAlbums()
    // Ensure we handle the response structure correctly
    // The previous code used res.data?.data, let's stick to that pattern or improve it
    albums.value = (res.data as any)?.data ?? [] 
  } catch (err) {
    console.error("Failed to fetch albums", err)
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

function openDeleteDialog(id: number) {
  albumToDelete.value = id
  deleteDialogOpen.value = true
}

async function handleConfirmDelete() {
  if (!albumToDelete.value) return
  
  deleting.value = true
  try {
    await deleteAlbum(albumToDelete.value)
    albums.value = albums.value.filter((a) => a.id !== albumToDelete.value)
    deleteDialogOpen.value = false
  } catch (e) {
    console.error(e)
  } finally {
    deleting.value = false
    albumToDelete.value = null
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

onMounted(fetchAlbums)
</script>

<template>
  <div class="space-y-8">
    <!-- Header Section -->
    <div class="flex flex-col md:flex-row md:items-center justify-between gap-4">
      <div>
        <h1 class="text-3xl font-serif font-bold tracking-tight text-primary">我的纪念册</h1>
        <p class="text-muted-foreground mt-1">记录珍贵的时光与回忆</p>
      </div>
      <div class="flex items-center gap-3">
        <div class="relative w-full md:w-64">
          <Search class="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            v-model="searchQuery"
            placeholder="搜索纪念册..."
            class="pl-9 bg-card"
          />
        </div>
        <Button @click="goCreate" class="gap-2 shadow-lg hover:shadow-xl transition-all">
          <Plus class="w-4 h-4" />
          新建纪念册
        </Button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <div v-for="i in 4" :key="i" class="h-64 rounded-xl bg-muted animate-pulse" />
    </div>

    <!-- Empty State -->
    <div v-else-if="albums.length === 0" class="flex flex-col items-center justify-center py-20 bg-card rounded-xl border border-dashed">
      <div class="w-16 h-16 rounded-full bg-muted flex items-center justify-center mb-4">
        <BookOpen class="w-8 h-8 text-muted-foreground" />
      </div>
      <h3 class="text-lg font-medium">还没有纪念册</h3>
      <p class="text-muted-foreground mb-6">开始创建你的第一本纪念册吧</p>
      <Button @click="goCreate">创建纪念册</Button>
    </div>

    <!-- Grid -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      <!-- Create New Card (Always first) -->
      <div 
        @click="goCreate"
        class="group relative flex flex-col items-center justify-center h-[320px] rounded-xl border-2 border-dashed border-muted hover:border-primary/50 hover:bg-accent/50 transition-all cursor-pointer"
      >
        <div class="w-12 h-12 rounded-full bg-muted group-hover:bg-primary/10 flex items-center justify-center transition-colors mb-3">
          <Plus class="w-6 h-6 text-muted-foreground group-hover:text-primary" />
        </div>
        <span class="font-medium text-muted-foreground group-hover:text-primary">新建纪念册</span>
      </div>

      <!-- Album Cards -->
      <AlbumCard
        v-for="album in filteredAlbums"
        :key="album.id"
        :album="album"
        @detail="goDetail"
        @delete="openDeleteDialog"
      />
    </div>

    <!-- Delete Confirmation Dialog -->
    <Dialog v-model:open="deleteDialogOpen">
      <DialogContent class="sm:max-w-[425px]">
        <DialogHeader>
          <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-red-100 mb-4">
            <AlertTriangle class="h-6 w-6 text-red-600" aria-hidden="true" />
          </div>
          <DialogTitle class="text-center">删除纪念册</DialogTitle>
          <DialogDescription class="text-center pt-2">
            您确定要永久删除这本纪念册吗？<br />
            此操作无法撤销，所有照片和内容将被清空。
          </DialogDescription>
        </DialogHeader>
        <DialogFooter class="sm:justify-center gap-2 mt-4">
          <Button variant="outline" @click="deleteDialogOpen = false" :disabled="deleting">
            取消
          </Button>
          <Button variant="destructive" @click="handleConfirmDelete" :disabled="deleting">
            {{ deleting ? '删除中...' : '确认删除' }}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  </div>
</template>
