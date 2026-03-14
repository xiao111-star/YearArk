<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getShareInfo, verifyAccessCode, uploadText, getMyUploads, deleteMyUpload } from '@/api/share'
import { getAnonTokenKey, setActiveAnonToken } from '@/utils/request'
import { useToast } from '@/components/ui/toast/use-toast'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const { toast } = useToast()
const inviteCode = route.params.inviteCode as string
const anonTokenKey = getAnonTokenKey(inviteCode)

const pageState = ref<'loading' | 'invalid' | 'needCode' | 'authenticated'>('loading')
const errorMsg = ref('')
const albumName = ref('')
const albumDes = ref('')
const accessCode = ref('')
const verifying = ref(false)
const verifyError = ref('')

// 已上传到后端的素材（图片）
const uploads = ref<Array<{ id: number; type: number; content: string }>>([])
const deletingIds = ref<Set<number>>(new Set())
const imageUploads = computed(() => uploads.value.filter(i => i.type === 2))

// 本地暂存的文字留言（未提交到后端）
const localTexts = ref<Array<{ localId: number; content: string }>>([])
let localIdCounter = 0
const textInput = ref('')
const editingId = ref<number | null>(null)
const editingContent = ref('')

// 提交状态
const submitting = ref(false)

onMounted(async () => {
  const existingToken = localStorage.getItem(anonTokenKey)
  if (existingToken) {
    setActiveAnonToken(existingToken)
    try {
      await loadMyUploads()
      pageState.value = 'authenticated'
      return
    } catch {
      localStorage.removeItem(anonTokenKey)
      setActiveAnonToken(null)
    }
  }
  await checkInvite()
})

async function checkInvite() {
  pageState.value = 'loading'
  try {
    const res = await getShareInfo(inviteCode)
    const info = res.data.data
    albumName.value = info.albumName
    albumDes.value = info.albumDes || ''
    pageState.value = 'needCode'
  } catch (err: any) {
    errorMsg.value = err.response?.data?.msg || '邀请链接无效或已过期'
    pageState.value = 'invalid'
  }
}

async function handleVerifyCode() {
  if (!accessCode.value.trim()) { verifyError.value = '请输入访问码'; return }
  verifying.value = true
  verifyError.value = ''
  try {
    const res = await verifyAccessCode(inviteCode, { accessCode: accessCode.value.trim() })
    const data = res.data.data
    localStorage.setItem(anonTokenKey, data.token)
    setActiveAnonToken(data.token)
    albumName.value = data.albumName || albumName.value
    await loadMyUploads()
    pageState.value = 'authenticated'
  } catch (err: any) {
    const msg = err.response?.data?.msg || '验证失败'
    if (pageState.value === 'needCode') verifyError.value = msg
    else { errorMsg.value = msg; pageState.value = 'invalid' }
  } finally {
    verifying.value = false
  }
}

async function loadMyUploads() {
  const res = await getMyUploads()
  uploads.value = res.data.data || []
}

function onImageUploaded() { loadMyUploads() }

async function handleDeleteMedia(id: number) {
  deletingIds.value.add(id)
  try {
    await deleteMyUpload(id)
    uploads.value = uploads.value.filter(u => u.id !== id)
  } catch (err: any) {
    console.error('删除失败', err)
  } finally {
    deletingIds.value.delete(id)
  }
}

// 本地留言操作
function addText() {
  const content = textInput.value.trim()
  if (!content) return
  localTexts.value.push({ localId: ++localIdCounter, content })
  textInput.value = ''
}

function removeText(localId: number) {
  localTexts.value = localTexts.value.filter(t => t.localId !== localId)
}

function startEdit(item: { localId: number; content: string }) {
  editingId.value = item.localId
  editingContent.value = item.content
}

function saveEdit(localId: number) {
  const content = editingContent.value.trim()
  if (!content) { removeText(localId); return }
  const item = localTexts.value.find(t => t.localId === localId)
  if (item) item.content = content
  editingId.value = null
  editingContent.value = ''
}

function cancelEdit() {
  editingId.value = null
  editingContent.value = ''
}

// 最终提交
const hasContent = computed(() => imageUploads.value.length > 0 || localTexts.value.length > 0)

async function handleFinalSubmit() {
  if (!hasContent.value) {
    toast({ description: '请至少上传一张图片或添加一条留言', variant: 'destructive' })
    return
  }
  submitting.value = true
  try {
    // 逐条提交本地暂存的文字留言
    for (const t of localTexts.value) {
      await uploadText({ content: t.content })
    }
    router.push({ path: `/share/${inviteCode}/complete`, query: { albumName: albumName.value } })
  } catch (err: any) {
    toast({ description: err.response?.data?.msg || '提交失败，请重试', variant: 'destructive' })
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <!-- Loading -->
  <div v-if="pageState === 'loading'" class="flex items-center justify-center py-20">
    <p class="text-muted-foreground">加载中...</p>
  </div>

  <!-- Invalid link -->
  <div v-else-if="pageState === 'invalid'" class="flex items-center justify-center py-20">
    <Card class="w-full">
      <CardHeader class="text-center">
        <CardTitle class="text-lg">无法访问</CardTitle>
      </CardHeader>
      <CardContent>
        <p class="text-center text-sm text-destructive">{{ errorMsg }}</p>
      </CardContent>
    </Card>
  </div>

  <!-- Access code input -->
  <div v-else-if="pageState === 'needCode'" class="flex items-center justify-center py-20">
    <Card class="w-full">
      <CardHeader class="text-center">
        <CardTitle class="text-lg">{{ albumName || '纪念册' }}</CardTitle>
        <p v-if="albumDes" class="text-sm text-muted-foreground mt-1">{{ albumDes }}</p>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="handleVerifyCode" class="space-y-4">
          <div class="space-y-2">
            <Label for="accessCode">请输入访问码</Label>
            <Input id="accessCode" v-model="accessCode" placeholder="输入访问码" />
          </div>
          <p v-if="verifyError" class="text-sm text-destructive">{{ verifyError }}</p>
          <Button type="submit" class="w-full" :disabled="verifying">
            {{ verifying ? '验证中...' : '进入' }}
          </Button>
        </form>
      </CardContent>
    </Card>
  </div>

  <!-- Authenticated: upload interface -->
  <div v-else class="space-y-6 pb-8">
    <!-- Header -->
    <div class="text-center">
      <h1 class="text-xl sm:text-2xl font-bold">{{ albumName }}</h1>
      <p v-if="albumDes" class="text-sm text-muted-foreground mt-1">{{ albumDes }}</p>
    </div>

    <!-- 图片区域 -->
    <Card>
      <CardHeader>
        <CardTitle class="text-base">上传图片 ({{ imageUploads.length }})</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
          <ImageUploader @uploaded="onImageUploaded" />
          <div
            v-for="item in imageUploads" :key="item.id"
            class="group relative aspect-square rounded-lg overflow-hidden border bg-muted"
          >
            <img :src="item.content" alt="已上传图片" class="h-full w-full object-cover" />
            <button
              class="absolute top-1 right-1 w-6 h-6 rounded-full bg-black/60 text-white flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity hover:bg-red-600"
              :disabled="deletingIds.has(item.id)" @click="handleDeleteMedia(item.id)" type="button" aria-label="删除图片"
            >
              <svg v-if="!deletingIds.has(item.id)" xmlns="http://www.w3.org/2000/svg" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
              </svg>
              <div v-else class="w-3 h-3 border border-white border-t-transparent rounded-full animate-spin" />
            </button>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 文字留言 -->
    <Card>
      <CardHeader>
        <CardTitle class="text-base">留言 ({{ localTexts.length }})</CardTitle>
      </CardHeader>
      <CardContent>
        <!-- 添加留言 -->
        <div class="flex gap-2">
          <textarea
            v-model="textInput" placeholder="写下你想说的话..." rows="2"
            class="flex-1 rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 resize-none"
            @keydown.enter.ctrl.prevent="addText"
          />
          <Button size="sm" class="self-end shrink-0" :disabled="!textInput.trim()" @click="addText">
            添加
          </Button>
        </div>

        <!-- 本地留言列表 -->
        <div v-if="localTexts.length > 0" class="mt-4 space-y-2">
          <div v-for="item in localTexts" :key="item.localId" class="group relative rounded-md border p-3">
            <!-- 编辑模式 -->
            <template v-if="editingId === item.localId">
              <textarea
                v-model="editingContent" rows="2"
                class="w-full rounded-md border border-input bg-background px-2 py-1 text-sm resize-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              <div class="flex gap-2 mt-2 justify-end">
                <Button size="sm" variant="outline" @click="cancelEdit">取消</Button>
                <Button size="sm" @click="saveEdit(item.localId)">保存</Button>
              </div>
            </template>
            <!-- 展示模式 -->
            <template v-else>
              <p class="text-sm text-muted-foreground whitespace-pre-wrap pr-14">{{ item.content }}</p>
              <div class="absolute top-2 right-2 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  class="w-5 h-5 rounded-full bg-muted text-muted-foreground flex items-center justify-center hover:bg-primary hover:text-white"
                  @click="startEdit(item)" type="button" aria-label="编辑留言"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                  </svg>
                </button>
                <button
                  class="w-5 h-5 rounded-full bg-muted text-muted-foreground flex items-center justify-center hover:bg-destructive hover:text-white"
                  @click="removeText(item.localId)" type="button" aria-label="删除留言"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" class="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </template>
          </div>
        </div>
      </CardContent>
    </Card>

    <!-- 提交按钮 -->
    <div class="pt-2">
      <Button
        class="w-full h-12 text-base"
        :disabled="submitting || !hasContent"
        @click="handleFinalSubmit"
      >
        {{ submitting ? '提交中...' : '完成' }}
      </Button>
      <p v-if="!hasContent" class="text-xs text-muted-foreground text-center mt-2">请至少上传一张图片或添加一条留言</p>
    </div>
  </div>
</template>
