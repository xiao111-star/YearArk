<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import MediaGrid from '@/components/MediaGrid.vue'
import MediaTextList from '@/components/MediaTextList.vue'
import InviteLinkItem from '@/components/InviteLinkItem.vue'
import type { MediaItem } from '@/components/MediaGrid.vue'
import type { Invite } from '@/components/InviteLinkItem.vue'
import { getAlbumDetail, generateAlbum, getAlbumStatus, uploadAlbumMedia } from '@/api/album'
import { listMedia, getMediaStats, deleteMedia } from '@/api/media'
import { listInvites, createInvite, disableInvite } from '@/api/invite'
import { useToast } from '@/components/ui/toast/use-toast'
import {
  ArrowLeft,
  Loader2,
  Image as ImageIcon,
  Type,
  Link2,
  Eye,
  Pencil,
  Sparkles,
  AlertTriangle,
} from 'lucide-vue-next'

const { toast } = useToast()
const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

/* ── State ── */
const album = ref<any>(null)
const mediaList = ref<MediaItem[]>([])
const mediaTab = ref<'image' | 'text'>('image')
const stats = ref({ imageCount: 0, textCount: 0 })
const invites = ref<Invite[]>([])

const newAccessCode = ref('')
const newExpireHours = ref(72)

const loading = ref(false)
const generating = ref(false)
const uploadingMedia = ref(false)
const creatingInvite = ref(false)

// Delete confirmation
const deleteDialogOpen = ref(false)
const mediaToDelete = ref<number | null>(null)
const deletingMedia = ref(false)

const images = computed(() => mediaList.value.filter((m) => m.type === 2))
const texts = computed(() => mediaList.value.filter((m) => m.type === 1))
const isProcessing = computed(() => album.value?.generationStatus === 1)
const isGenerated = computed(() => album.value?.generationStatus === 2)
const generationFailed = computed(() => album.value?.generationStatus === 3)

/* ── Polling ── */
let pollTimer: ReturnType<typeof setTimeout> | null = null
const POLL_INTERVAL = 3000
const POLL_MAX = 60
let pollCount = 0

function startPolling() {
  stopPolling()
  pollCount = 0
  poll()
}

function stopPolling() {
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

async function poll() {
  if (pollCount >= POLL_MAX) {
    stopPolling()
    return
  }
  pollCount++
  try {
    const res = await getAlbumStatus(albumId)
    const updated = res.data?.data
    if (updated) album.value = { ...album.value!, ...updated }
    if (updated?.generationStatus === 2 || updated?.generationStatus === 3) {
      stopPolling()
    } else {
      pollTimer = setTimeout(poll, POLL_INTERVAL)
    }
  } catch {
    pollTimer = setTimeout(poll, POLL_INTERVAL)
  }
}

/* ── Data Fetching ── */
async function fetchAlbum() {
  try {
    const res = await getAlbumDetail(albumId)
    album.value = res.data?.data ?? null
  } catch {
    album.value = null
  }
}

async function fetchMedia() {
  try {
    const [mediaRes, statsRes] = await Promise.all([
      listMedia({ albumId }),
      getMediaStats(albumId),
    ])
    mediaList.value = mediaRes.data?.data ?? []
    stats.value = statsRes.data?.data ?? { imageCount: 0, textCount: 0 }
  } catch {
    mediaList.value = []
  }
}

async function fetchInvites() {
  try {
    const res = await listInvites(albumId)
    invites.value = res.data?.data ?? []
  } catch {
    invites.value = []
  }
}

/* ── Actions ── */
async function handleGenerate() {
  generating.value = true
  try {
    await generateAlbum(albumId)
    await fetchAlbum()
    startPolling()
    toast({ description: '纪念册生成已启动' })
  } catch {
    toast({ description: '生成请求失败，请重试', variant: 'destructive' })
  } finally {
    generating.value = false
  }
}

async function handleMediaUpload(files: File[]) {
  uploadingMedia.value = true
  let success = 0
  for (const file of files) {
    try {
      await uploadAlbumMedia(albumId, file)
      success++
    } catch {}
  }
  await fetchMedia()
  uploadingMedia.value = false
  if (success > 0) toast({ description: `已上传 ${success} 张照片` })
  if (success < files.length) toast({ description: `${files.length - success} 张上传失败`, variant: 'destructive' })
}

function confirmDeleteMedia(mediaId: number) {
  mediaToDelete.value = mediaId
  deleteDialogOpen.value = true
}

async function handleConfirmDeleteMedia() {
  if (!mediaToDelete.value) return
  deletingMedia.value = true
  try {
    await deleteMedia(mediaToDelete.value)
    await fetchMedia()
    toast({ description: '已删除素材' })
    deleteDialogOpen.value = false
  } catch {
    toast({ description: '删除失败，请重试', variant: 'destructive' })
  } finally {
    deletingMedia.value = false
    mediaToDelete.value = null
  }
}

async function handleCreateInvite() {
  if (!newAccessCode.value.trim()) {
    toast({ description: '请输入访问码', variant: 'destructive' })
    return
  }
  creatingInvite.value = true
  try {
    const expireAt = new Date()
    expireAt.setHours(expireAt.getHours() + newExpireHours.value)
    await createInvite({
      albumId,
      accessCode: newAccessCode.value.trim(),
      expireAt: expireAt.toISOString(),
    })
    newAccessCode.value = ''
    await fetchInvites()
    toast({ description: '邀请链接已生成' })
  } catch {
    toast({ description: '生成邀请链接失败', variant: 'destructive' })
  } finally {
    creatingInvite.value = false
  }
}

async function handleDisableInvite(id: number) {
  try {
    await disableInvite(id)
    await fetchInvites()
    toast({ description: '已禁用邀请链接' })
  } catch {
    toast({ description: '操作失败', variant: 'destructive' })
  }
}

/* ── Lifecycle ── */
onMounted(async () => {
  loading.value = true
  await Promise.all([fetchAlbum(), fetchMedia(), fetchInvites()])
  loading.value = false
  if (album.value?.generationStatus === 1) startPolling()
})

onUnmounted(() => stopPolling())
</script>

<template>
  <!-- Loading -->
  <div v-if="loading" class="flex items-center justify-center py-20">
    <Loader2 class="w-6 h-6 animate-spin text-muted-foreground" />
  </div>

  <!-- Not Found -->
  <div v-else-if="!album" class="flex flex-col items-center justify-center py-20">
    <p class="text-muted-foreground mb-4">纪念册不存在或已被删除</p>
    <Button variant="outline" @click="router.push('/dashboard')">返回首页</Button>
  </div>

  <!-- Main Content -->
  <div v-else class="space-y-6">
    <!-- Back -->
    <Button
      variant="ghost"
      size="sm"
      class="gap-1.5 -ml-2 text-muted-foreground hover:text-foreground"
      @click="router.push('/dashboard')"
    >
      <ArrowLeft class="w-4 h-4" />
      返回
    </Button>

    <!-- Header Card -->
    <div class="rounded-xl border bg-card p-6">
      <div class="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <!-- Info -->
        <div class="min-w-0 flex-1">
          <div class="flex items-center gap-3 mb-2">
            <h1 class="text-2xl font-serif font-bold tracking-tight text-primary truncate">
              {{ album.name }}
            </h1>
            <span
              class="shrink-0 px-2.5 py-0.5 text-xs font-medium rounded-full"
              :class="
                album.status === 1
                  ? 'bg-green-100 text-green-700'
                  : 'bg-secondary text-secondary-foreground'
              "
            >
              {{ album.status === 1 ? '已发布' : '草稿' }}
            </span>
          </div>
          <p class="text-sm text-muted-foreground">{{ album.des || '暂无描述' }}</p>

          <!-- Generation status banner -->
          <div
            v-if="isProcessing"
            class="mt-3 flex items-center gap-2 text-sm text-amber-600 bg-amber-50 rounded-lg px-3 py-2"
          >
            <Loader2 class="w-4 h-4 animate-spin" />
            纪念册正在生成中，请稍候...
          </div>
          <div
            v-else-if="generationFailed"
            class="mt-3 flex items-center gap-2 text-sm text-destructive bg-red-50 rounded-lg px-3 py-2"
          >
            <AlertTriangle class="w-4 h-4" />
            生成失败：{{ album.generationFailReason || '未知错误' }}
          </div>
        </div>

        <!-- Actions -->
        <div class="flex flex-wrap items-center gap-2 shrink-0">
          <Button
            variant="outline"
            size="sm"
            class="gap-1.5"
            @click="router.push(`/album/${albumId}/preview`)"
          >
            <Eye class="w-3.5 h-3.5" />
            预览
          </Button>
          <Button
            v-if="isGenerated"
            variant="outline"
            size="sm"
            class="gap-1.5"
            @click="router.push(`/album/${albumId}/edit`)"
          >
            <Pencil class="w-3.5 h-3.5" />
            编辑
          </Button>
          <Button
            size="sm"
            class="gap-1.5"
            :disabled="generating || isProcessing"
            @click="handleGenerate"
          >
            <Loader2 v-if="isProcessing" class="w-3.5 h-3.5 animate-spin" />
            <Sparkles v-else class="w-3.5 h-3.5" />
            {{ isProcessing ? '生成中...' : '生成纪念册' }}
          </Button>
        </div>
      </div>
    </div>

    <!-- Media & Invite Section (Grid Layout) -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
      <!-- Media Section (Left, takes 2 columns) -->
      <Card class="lg:col-span-2 flex flex-col h-full">
        <CardHeader>
          <div class="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div class="flex items-center gap-2">
              <CardTitle class="text-lg">素材管理</CardTitle>
              <span class="text-xs text-muted-foreground bg-muted px-2 py-0.5 rounded-full">
                {{ stats.imageCount }} 图 · {{ stats.textCount }} 文
              </span>
            </div>
            <div class="flex rounded-lg border p-0.5 bg-muted/50 self-start sm:self-auto">
              <button
                class="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md transition-colors"
                :class="
                  mediaTab === 'image'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                "
                @click="mediaTab = 'image'"
              >
                <ImageIcon class="w-3.5 h-3.5" />
                图片
              </button>
              <button
                class="flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md transition-colors"
                :class="
                  mediaTab === 'text'
                    ? 'bg-background text-foreground shadow-sm'
                    : 'text-muted-foreground hover:text-foreground'
                "
                @click="mediaTab = 'text'"
              >
                <Type class="w-3.5 h-3.5" />
                文字
              </button>
            </div>
          </div>
        </CardHeader>
        <CardContent class="flex-1 min-h-[400px]">
          <MediaGrid
            v-if="mediaTab === 'image'"
            :images="images"
            :uploading="uploadingMedia"
            @upload="handleMediaUpload"
            @delete="confirmDeleteMedia"
          />
          <MediaTextList v-else :texts="texts" />
        </CardContent>
      </Card>

      <!-- Invite Section (Right, takes 1 column) -->
      <Card class="flex flex-col h-full">
        <CardHeader>
          <div class="flex items-center gap-2">
            <Link2 class="w-4 h-4 text-muted-foreground" />
            <CardTitle class="text-lg">邀请链接</CardTitle>
          </div>
        </CardHeader>
        <CardContent class="space-y-4 flex-1">
          <!-- Existing invites -->
          <div v-if="invites.length > 0" class="space-y-3">
            <InviteLinkItem
              v-for="inv in invites"
              :key="inv.id"
              :invite="inv"
              @disable="handleDisableInvite"
            />
          </div>
          <div v-else class="flex flex-col items-center justify-center py-8 text-center px-4">
            <div class="w-12 h-12 rounded-full bg-muted flex items-center justify-center mb-3">
              <Link2 class="w-6 h-6 text-muted-foreground/50" />
            </div>
            <p class="text-sm text-muted-foreground">暂无邀请链接</p>
            <p class="text-xs text-muted-foreground/70 mt-1">创建一个分享给朋友，一起完善纪念册</p>
          </div>

          <!-- Create form -->
          <div class="mt-auto pt-6 border-t space-y-3">
            <h4 class="text-sm font-medium">创建新链接</h4>
            <div class="space-y-3">
              <div class="space-y-1.5">
                <Label class="text-xs text-muted-foreground">访问码</Label>
                <Input
                  v-model="newAccessCode"
                  placeholder="设置访问码"
                  class="h-9 text-sm"
                />
              </div>
              <div class="space-y-1.5">
                <Label class="text-xs text-muted-foreground">有效期（小时）</Label>
                <Input
                  v-model.number="newExpireHours"
                  type="number"
                  :min="1"
                  class="h-9 text-sm"
                />
              </div>
              <Button class="w-full" size="sm" :disabled="creatingInvite" @click="handleCreateInvite">
                {{ creatingInvite ? '生成中...' : '生成链接' }}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  </div>

  <!-- Delete Media Confirmation Dialog -->
  <Dialog v-model:open="deleteDialogOpen">
    <DialogContent class="sm:max-w-[380px]">
      <DialogHeader>
        <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-red-100 mb-3">
          <AlertTriangle class="h-5 w-5 text-red-600" />
        </div>
        <DialogTitle class="text-center">删除素材</DialogTitle>
        <DialogDescription class="text-center pt-1">
          确定要删除这个素材吗？此操作无法撤销。
        </DialogDescription>
      </DialogHeader>
      <DialogFooter class="sm:justify-center gap-2 mt-2">
        <Button
          variant="outline"
          size="sm"
          :disabled="deletingMedia"
          @click="deleteDialogOpen = false"
        >
          取消
        </Button>
        <Button
          variant="destructive"
          size="sm"
          :disabled="deletingMedia"
          @click="handleConfirmDeleteMedia"
        >
          {{ deletingMedia ? '删除中...' : '确认删除' }}
        </Button>
      </DialogFooter>
    </DialogContent>
  </Dialog>
</template>
