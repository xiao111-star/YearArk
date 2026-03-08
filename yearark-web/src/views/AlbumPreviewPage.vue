<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Button } from '@/components/ui/button'
import BookViewer from '@/components/BookViewer.vue'
import PageEditor from '@/components/PageEditor.vue'
import MediaPanel from '@/components/MediaPanel.vue'
import type { PageItem } from '@/components/BookViewer.vue'
import type { EditablePage } from '@/components/PageEditor.vue'
import type { UnusedMedia } from '@/components/MediaPanel.vue'
import { previewAlbum, getAlbumEditData, updatePageData, batchUpdatePages, getUnusedMedia } from '@/api/album'

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

// Read-only preview
const pages = ref<PageItem[]>([])
const loading = ref(false)

// Edit mode
const editMode = ref(route.query.edit === '1')
const editPages = ref<EditablePage[]>([])
const unusedMedia = ref<UnusedMedia[]>([])
const editLoading = ref(false)
const saving = ref(false)
const saveErrors = ref<Record<number, string>>({})

// Track local edits: pageId -> updated dataMap
const pendingEdits = ref<Map<number, Record<string, unknown>>>(new Map())

const currentEditPageIndex = ref(0)
const currentEditPage = computed(() => editPages.value[currentEditPageIndex.value] ?? null)

async function fetchPages() {
  loading.value = true
  try {
    const res = await previewAlbum(albumId)
    pages.value = res.data?.data ?? []
  } catch { pages.value = [] } finally { loading.value = false }
}

async function fetchEditData() {
  editLoading.value = true
  try {
    const [editRes, mediaRes] = await Promise.all([
      getAlbumEditData(albumId),
      getUnusedMedia(albumId),
    ])
    editPages.value = editRes.data?.data?.pages ?? []
    unusedMedia.value = mediaRes.data?.data ?? []
  } catch { /* interceptor */ } finally { editLoading.value = false }
}

async function toggleEditMode() {
  editMode.value = !editMode.value
  if (editMode.value && editPages.value.length === 0) {
    await fetchEditData()
  }
  pendingEdits.value = new Map()
  saveErrors.value = {}
}

function onPageUpdate(pageId: number, data: Record<string, unknown>) {
  pendingEdits.value.set(pageId, data)
}

async function handleSave() {
  if (pendingEdits.value.size === 0) { editMode.value = false; return }
  saving.value = true
  saveErrors.value = {}
  try {
    const updates = Array.from(pendingEdits.value.entries()).map(([pageId, dataMap]) => ({ pageId, dataMap }))
    await batchUpdatePages(albumId, updates)
    pendingEdits.value = new Map()
    // Refresh both views
    await Promise.all([fetchPages(), fetchEditData()])
    editMode.value = false
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { msg?: string } } })?.response?.data?.msg ?? '保存失败，请检查数据'
    saveErrors.value[-1] = msg
  } finally { saving.value = false }
}

function goBack() { router.push(`/album/${albumId}`) }

onMounted(async () => {
  await fetchPages()
  if (editMode.value) await fetchEditData()
})
</script>

<template>
  <div>
    <!-- Top bar -->
    <div class="mb-4 flex items-center justify-between">
      <Button variant="ghost" size="sm" @click="goBack">← 返回详情</Button>
      <div class="flex gap-2">
        <Button
          v-if="!editMode"
          variant="outline"
          size="sm"
          @click="toggleEditMode"
        >进入编辑模式</Button>
        <template v-else>
          <Button variant="outline" size="sm" :disabled="saving" @click="toggleEditMode">取消</Button>
          <Button size="sm" :disabled="saving" @click="handleSave">
            {{ saving ? '保存中...' : '保存' }}
          </Button>
        </template>
      </div>
    </div>

    <!-- Global save error -->
    <div v-if="saveErrors[-1]" class="mb-4 rounded-md bg-red-50 border border-red-200 px-4 py-2 text-sm text-red-700">
      {{ saveErrors[-1] }}
    </div>

    <!-- Loading -->
    <div v-if="loading || editLoading" class="text-center py-12 text-muted-foreground">加载中...</div>

    <!-- Empty -->
    <div v-else-if="!editMode && pages.length === 0" class="text-center py-20">
      <p class="text-muted-foreground mb-4">纪念册尚未生成，请先点击生成</p>
      <Button @click="goBack">返回详情页</Button>
    </div>

    <!-- Read-only preview -->
    <BookViewer v-else-if="!editMode" :pages="pages" />

    <!-- Edit mode -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-4">
      <!-- Page list (left) -->
      <div class="lg:col-span-1 space-y-2">
        <p class="text-sm font-medium text-muted-foreground">页面列表</p>
        <button
          v-for="(p, idx) in editPages"
          :key="p.pageId"
          :class="[
            'w-full text-left px-3 py-2 rounded-md text-sm border transition-colors',
            idx === currentEditPageIndex
              ? 'bg-primary text-primary-foreground border-primary'
              : 'hover:bg-muted border-transparent',
          ]"
          @click="currentEditPageIndex = idx"
        >
          第 {{ p.sort }} 页
        </button>
      </div>

      <!-- Editor (center) -->
      <div class="lg:col-span-1">
        <div v-if="currentEditPage" class="space-y-3">
          <p class="text-sm font-medium">第 {{ currentEditPage.sort }} 页</p>
          <PageEditor
            :page="currentEditPage"
            :unused-media="unusedMedia"
            @update="onPageUpdate(currentEditPage.pageId, $event)"
          />
        </div>
      </div>

      <!-- Media panel (right) -->
      <div class="lg:col-span-1">
        <MediaPanel :media="unusedMedia" @drag-start="() => {}" />
      </div>
    </div>
  </div>
</template>
