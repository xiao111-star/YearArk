<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import MediaGrid from '@/components/MediaGrid.vue'
import MediaTextList from '@/components/MediaTextList.vue'
import InviteLinkItem from '@/components/InviteLinkItem.vue'
import type { MediaItem } from '@/components/MediaGrid.vue'
import type { Invite } from '@/components/InviteLinkItem.vue'
import { getAlbumDetail, generateAlbum, getAlbumStatus } from '@/api/album'
import { listMedia, getMediaStats } from '@/api/media'
import { listInvites, createInvite, disableInvite } from '@/api/invite'
import {
  ArrowLeft, Loader2, Eye, Wand2, Image, Type,
  Link2, Clock, BookOpen, AlertTriangle, CheckCircle2
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

// Album info
const album = ref<any>(null)

// Media
const mediaList = ref<MediaItem[]>([])
const mediaTab = ref<'image' | 'text'>('image')
const stats = ref({ imageCount: 0, textCount: 0 })
const images = computed(() => mediaList.value.filter((m) => m.type === 2))
const texts = computed(() => mediaList.value.filter((m) => m.type === 1))

// Invites
const invites = ref<Invite[]>([])
const newAccessCode = ref('')
const newExpireHours = ref(72)
const creatingInvite = ref(false)

// Loading
const loading = ref(false)
const generating = ref(false)

// 轮询
let pollTimer: ReturnType<typeof setTimeout> | null = null
const POLL_INTERVAL = 3000
const POLL_MAX = 60
let pollCount = 0

const isProcessing = computed(() => album.value?.generationStatus === 1)
const generationFailed = computed(() => album.value?.generationStatus === 3)
const generationDone = computed(() => album.value?.generationStatus === 2)

const statusLabel = computed(() => {
  if (!album.value) return ''
  if (isProcessing.value) return '生成中'
  if (generationDone.value) return '已生成'
  if (generationFailed.value) return '生成失败'
  return album.value.status === 1 ? '已发布' : '草稿'
})

const statusClass = computed(() => {
  if (isProcessing.value) return 'bg-amber-100 text-amber-700'
  if (generationDone.value) return 'bg-green-100 text-green-700'
  if (generationFailed.value) return 'bg-red-100 text-red-700'
  return album.value?.status === 1 ? 'bg-green-100 text-green-700' : 'bg-secondary text-secondary-foreground'
})

async function fetchAlbum() {
  try {
    const res = await getAlbumDetail(albumId)
    album.value = res.data?.data ?? null
  } catch { album.value = null }
}

async function fetchMedia() {
  try {
    const [mediaRes, statsRes] = await Promise.all([
      listMedia({ albumId }),
      getMediaStats(albumId),
    ])
    mediaList.value = mediaRes.data?.data ?? []
    stats.value = statsRes.data?.data ?? { imageCount: 0, textCount: 0 }
  } catch { mediaList.value = [] }
}

async function fetchInvites() {
  try {
    const res = await listInvites(albumId)
    invites.value = res.data?.data ?? []
  } catch { invites.value = [] }
}

async function handleCreateInvite() {
  if (!newAccessCode.value.trim()) return
  creatingInvite.value = true
  try {
    const expireAt = new Date()
    expireAt.setHours(expireAt.getHours() + newExpireHours.value)
    await createInvite({ albumId, accessCode: newAccessCode.value.trim(), expireAt: expireAt.toISOString() })
    newAccessCode.value = ''
    await fetchInvites()
  } catch {} finally { creatingInvite.value = false }
}

async function handleDisableInvite(id: number) {
  try { await disableInvite(id); await fetchInvites() } catch {}
}

async function handleGenerate() {
  generating.value = true
  try { await generateAlbum(albumId); await fetchAlbum(); startPolling() }
  catch {} finally { generating.value = false }
}

function startPolling() { stopPolling(); pollCount = 0; poll() }
function stopPolling() { if (pollTimer) { clearTimeout(pollTimer); pollTimer = null } }

async function poll() {
  if (pollCount >= POLL_MAX) { stopPolling(); return }
  pollCount++
  try {
    const res = await getAlbumStatus(albumId)
    const updated = res.data?.data
    if (updated) album.value = { ...album.value!, ...updated }
    if (updated?.generationStatus === 2 || updated?.generationStatus === 3) stopPolling()
    else pollTimer = setTimeout(poll, POLL_INTERVAL)
  } catch { pollTimer = setTimeout(poll, POLL_INTERVAL) }
}

onUnmounted(() => stopPolling())

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchAlbum(), fetchMedia(), fetchInvites()])
  loading.value = false
  if (album.value?.generationStatus === 1) startPolling()
})
</script>

<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="space-y-6">
      <div class="h-10 w-48 rounded-lg bg-muted animate-pulse" />
      <div class="h-32 rounded-xl bg-muted animate-pulse" />
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 h-64 rounded-xl bg-muted animate-pulse" />
        <div class="h-64 rounded-xl bg-muted animate-pulse" />
      </div>
    </div>

    <!-- Not found -->
    <div v-else-if="!album" class="flex flex-col items-center py-20">
      <BookOpen class="w-12 h-12 text-muted-foreground mb-4" />
      <p class="text-muted-foreground">纪念册不存在</p>
      <Button variant="outline" class="mt-4" @click="router.push('/dashboard')">返回首页</Button>
    </div>

    <template v-else>
      <!-- Breadcrumb -->
      <div class="mb-6">
        <Button variant="ghost" size="sm" class="gap-1.5 -ml-2 text-muted-foreground hover:text-primary" @click="router.push('/dashboard')">
          <ArrowLeft class="w-3.5 h-3.5" />
          我的纪念册
        </Button>
      </div>

      <!-- Hero Header Card -->
      <div class="rounded-xl border bg-card overflow-hidden mb-8">
        <!-- Top gradient bar -->
        <div class="h-2 bg-gradient-to-r from-primary/60 via-primary/30 to-accent/40" />
        <div class="p-6 md:p-8">
          <div class="flex flex-col md:flex-row md:items-start justify-between gap-4">
            <div class="flex-1 min-w-0">
              <div class="flex items-center gap-3 mb-2">
                <h1 class="text-2xl md:text-3xl font-serif font-bold tracking-tight text-foreground truncate">
                  {{ album.name }}
                </h1>
                <span class="shrink-0 px-2.5 py-0.5 text-xs font-medium rounded-full" :class="statusClass">
                  {{ statusLabel }}
                </span>
              </div>
              <p class="text-muted-foreground leading-relaxed">{{ album.des || '暂无描述' }}</p>

              <!-- Generation failure message -->
              <div v-if="generationFailed" class="mt-3 flex items-center gap-2 text-sm text-destructive bg-destructive/10 rounded-lg px-3 py-2">
                <AlertTriangle class="w-4 h-4 shrink-0" />
                {{ album.generationFailReason || '生成失败，请重试' }}
              </div>

              <!-- Quick stats -->
              <div class="flex items-center gap-6 mt-4 text-sm text-muted-foreground">
                <div class="flex items-center gap-1.5">
                  <Image class="w-4 h-4" />
                  <span>{{ stats.imageCount }} 张图片</span>
                </div>
                <div class="flex items-center gap-1.5">
                  <Type class="w-4 h-4" />
                  <span>{{ stats.textCount }} 条文字</span>
                </div>
                <div class="flex items-center gap-1.5">
                  <Link2 class="w-4 h-4" />
                  <span>{{ invites.length }} 个邀请</span>
                </div>
              </div>
            </div>

            <!-- Action buttons -->
            <div class="flex items-center gap-2 shrink-0">
              <Button
                variant="outline"
                :disabled="isProcessing"
                @click="router.push(`/album/${albumId}/preview`)"
                class="gap-2"
              >
                <Eye class="w-4 h-4" />
                预览
              </Button>
              <Button @click="handleGenerate" :disabled="generating || isProcessing" class="gap-2">
                <Loader2 v-if="isProcessing" class="w-4 h-4 animate-spin" />
                <Wand2 v-else class="w-4 h-4" />
                {{ isProcessing ? '生成中...' : '生成纪念册' }}
              </Button>
            </div>
          </div>
        </div>
      </div>

      <!-- Two-column layout -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Left: Media (2/3 width) -->
        <div class="lg:col-span-2 space-y-6">
          <Card>
            <CardHeader class="pb-3">
              <div class="flex items-center justify-between">
                <CardTitle class="text-lg flex items-center gap-2">
                  <Image class="w-5 h-5 text-primary" />
                  素材管理
                </CardTitle>
              </div>
              <!-- Tabs -->
              <div class="flex gap-2 mt-3">
                <button
                  class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors"
                  :class="mediaTab === 'image'
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-secondary text-secondary-foreground hover:bg-accent'"
                  @click="mediaTab = 'image'"
                >
                  图片 ({{ stats.imageCount }})
                </button>
                <button
                  class="px-4 py-1.5 rounded-lg text-sm font-medium transition-colors"
                  :class="mediaTab === 'text'
                    ? 'bg-primary text-primary-foreground'
                    : 'bg-secondary text-secondary-foreground hover:bg-accent'"
                  @click="mediaTab = 'text'"
                >
                  文字 ({{ stats.textCount }})
                </button>
              </div>
            </CardHeader>
            <CardContent>
              <MediaGrid v-if="mediaTab === 'image'" :images="images" />
              <MediaTextList v-else :texts="texts" />
            </CardContent>
          </Card>
        </div>

        <!-- Right: Invites (1/3 width) -->
        <div class="space-y-6">
          <Card>
            <CardHeader class="pb-3">
              <CardTitle class="text-lg flex items-center gap-2">
                <Link2 class="w-5 h-5 text-primary" />
                邀请链接
              </CardTitle>
            </CardHeader>
            <CardContent class="space-y-4">
              <!-- Existing links -->
              <div v-if="invites.length > 0" class="space-y-3">
                <InviteLinkItem
                  v-for="inv in invites"
                  :key="inv.id"
                  :invite="inv"
                  @disable="handleDisableInvite"
                />
              </div>
              <p v-else class="text-sm text-muted-foreground py-4 text-center">
                还没有邀请链接，创建一个分享给同学吧
              </p>

              <!-- Create form -->
              <div class="pt-4 border-t space-y-3">
                <div class="space-y-1.5">
                  <Label class="text-xs text-muted-foreground">访问码</Label>
                  <Input v-model="newAccessCode" placeholder="设置访问码" />
                </div>
                <div class="space-y-1.5">
                  <Label class="text-xs text-muted-foreground">有效期（小时）</Label>
                  <Input v-model.number="newExpireHours" type="number" :min="1" />
                </div>
                <Button class="w-full" :disabled="creatingInvite || !newAccessCode.trim()" @click="handleCreateInvite">
                  {{ creatingInvite ? '生成中...' : '生成邀请链接' }}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </template>
  </div>
</template>
