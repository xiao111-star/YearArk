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

const route = useRoute()
const router = useRouter()
const albumId = Number(route.params.id)

// Album info
const album = ref<{
  id: number; name: string; des: string; templateId: number; status: number; createTime: string
} | null>(null)

// Generation status
const generationStatus = ref<'pending' | 'processing' | 'completed' | 'failed' | null>(null)
const generationFailReason = ref<string | null>(null)
let pollTimer: ReturnType<typeof setInterval> | null = null

const statusLabel: Record<string, string> = {
  pending: '待生成',
  processing: '生成中...',
  completed: '已生成',
  failed: '生成失败',
}
const statusClass: Record<string, string> = {
  pending: 'bg-gray-100 text-gray-600',
  processing: 'bg-blue-100 text-blue-700 animate-pulse',
  completed: 'bg-green-100 text-green-700',
  failed: 'bg-red-100 text-red-700',
}

async function fetchGenerationStatus() {
  try {
    const res = await getAlbumStatus(albumId)
    const d = res.data?.data
    generationStatus.value = d?.generationStatus ?? null
    generationFailReason.value = d?.failReason ?? null
    if (generationStatus.value === 'completed' || generationStatus.value === 'failed') {
      stopPolling()
    }
  } catch { /* ignore */ }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(fetchGenerationStatus, 5000)
}

function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}

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
  if (!newAccessCode.value.trim()) { alert('请输入访问码'); return }
  creatingInvite.value = true
  try {
    const expireAt = new Date()
    expireAt.setHours(expireAt.getHours() + newExpireHours.value)
    await createInvite({ albumId, accessCode: newAccessCode.value.trim(), expireAt: expireAt.toISOString() })
    newAccessCode.value = ''
    await fetchInvites()
  } catch { /* interceptor */ } finally { creatingInvite.value = false }
}

async function handleDisableInvite(id: number) {
  try { await disableInvite(id); await fetchInvites() } catch { /* interceptor */ }
}

async function handleGenerate() {
  generating.value = true
  try {
    await generateAlbum(albumId)
    generationStatus.value = 'processing'
    generationFailReason.value = null
    startPolling()
  } catch { /* interceptor */ } finally { generating.value = false }
}

function goPreview() { router.push(`/album/${albumId}/preview`) }
function goEdit() { router.push(`/album/${albumId}/preview?edit=1`) }

onMounted(async () => {
  loading.value = true
  await Promise.all([fetchAlbum(), fetchMedia(), fetchInvites(), fetchGenerationStatus()])
  loading.value = false
  if (generationStatus.value === 'processing') startPolling()
})

onUnmounted(() => stopPolling())
</script>

<template>
  <div>
    <!-- Loading -->
    <div v-if="loading" class="text-center py-12 text-muted-foreground">加载中...</div>

    <!-- Not found -->
    <div v-else-if="!album" class="text-center py-12 text-muted-foreground">纪念册不存在</div>

    <template v-else>
      <!-- Album Info -->
      <Card class="mb-6">
        <CardHeader>
          <div class="flex items-center justify-between">
            <CardTitle class="text-xl">{{ album.name }}</CardTitle>
            <span
              :class="[
                'inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium',
                album.status === 1 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-600',
              ]"
            >
              {{ album.status === 1 ? '已发布' : '草稿' }}
            </span>
          </div>
        </CardHeader>
        <CardContent>
          <p class="text-sm text-muted-foreground">{{ album.des || '暂无描述' }}</p>
        </CardContent>
      </Card>

      <!-- Media Section -->
      <Card class="mb-6">
        <CardHeader>
          <div class="flex items-center justify-between">
            <CardTitle class="text-lg">素材管理</CardTitle>
            <span class="text-sm text-muted-foreground">
              图片 {{ stats.imageCount }} 张 · 文字 {{ stats.textCount }} 条
            </span>
          </div>
          <!-- Tabs -->
          <div class="flex gap-2 mt-3">
            <Button
              size="sm"
              :variant="mediaTab === 'image' ? 'default' : 'outline'"
              @click="mediaTab = 'image'"
            >
              图片
            </Button>
            <Button
              size="sm"
              :variant="mediaTab === 'text' ? 'default' : 'outline'"
              @click="mediaTab = 'text'"
            >
              文字
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <MediaGrid v-if="mediaTab === 'image'" :images="images" />
          <MediaTextList v-else :texts="texts" />
        </CardContent>
      </Card>

      <!-- Invite Links Section -->
      <Card class="mb-6">
        <CardHeader>
          <CardTitle class="text-lg">邀请链接</CardTitle>
        </CardHeader>
        <CardContent>
          <!-- Existing links -->
          <div v-if="invites.length > 0" class="space-y-3 mb-4">
            <InviteLinkItem
              v-for="inv in invites"
              :key="inv.id"
              :invite="inv"
              @disable="handleDisableInvite"
            />
          </div>
          <p v-else class="text-sm text-muted-foreground mb-4">暂无邀请链接</p>

          <!-- Create new link form -->
          <div class="flex flex-wrap items-end gap-3 pt-3 border-t">
            <div class="space-y-1">
              <Label class="text-xs">访问码（必填）</Label>
              <Input
                v-model="newAccessCode"
                placeholder="请输入访问码"
                class="w-40"
              />
            </div>
            <div class="space-y-1">
              <Label class="text-xs">有效期（小时）</Label>
              <Input
                v-model.number="newExpireHours"
                type="number"
                :min="1"
                class="w-28"
              />
            </div>
            <Button :disabled="creatingInvite" @click="handleCreateInvite">
              {{ creatingInvite ? '生成中...' : '生成邀请链接' }}
            </Button>
          </div>
        </CardContent>
      </Card>

      <!-- Action Buttons -->
      <div class="flex gap-3">
        <Button :disabled="generating || generationStatus === 'processing'" @click="handleGenerate">
          {{ generating || generationStatus === 'processing' ? '生成中...' : (generationStatus === 'completed' ? '重新生成' : '生成纪念册') }}
        </Button>
        <Button
          variant="outline"
          :disabled="generationStatus !== 'completed'"
          @click="goPreview"
        >预览纪念册</Button>
        <Button
          variant="outline"
          :disabled="generationStatus !== 'completed'"
          @click="goEdit"
        >编辑纪念册</Button>
      </div>

      <!-- Generation Status -->
      <div v-if="generationStatus" class="mt-4">
        <span :class="['inline-flex items-center rounded-full px-3 py-1 text-sm font-medium', statusClass[generationStatus]]">
          {{ statusLabel[generationStatus] }}
        </span>
        <p v-if="generationStatus === 'failed' && generationFailReason" class="mt-2 text-sm text-red-600">
          失败原因：{{ generationFailReason }}
        </p>
      </div>
    </template>
  </div>
</template>
