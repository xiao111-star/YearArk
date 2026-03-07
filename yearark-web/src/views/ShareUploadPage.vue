<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getShareInfo, verifyAccessCode, uploadText, getMyUploads } from '@/api/share'
import { getAnonTokenKey, setActiveAnonToken } from '@/utils/request'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const inviteCode = route.params.inviteCode as string
const anonTokenKey = getAnonTokenKey(inviteCode)

// 页面状态: loading / invalid / needCode / authenticated
const pageState = ref<'loading' | 'invalid' | 'needCode' | 'authenticated'>('loading')
const errorMsg = ref('')
const albumName = ref('')
const albumDes = ref('')

// 访问码
const accessCode = ref('')
const verifying = ref(false)
const verifyError = ref('')

// 文字留言
const textContent = ref('')
const submittingText = ref(false)
const textMsg = ref('')
const textMsgType = ref<'success' | 'error'>('')

// 已上传素材
const uploads = ref<Array<{ id: number; type: number; content: string }>>([])

onMounted(async () => {
  // 如果已有该邀请码对应的 token，尝试直接进入上传页
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
    
    // 访问码必填，显示输入页面
    pageState.value = 'needCode'
  } catch (err: any) {
    errorMsg.value = err.response?.data?.msg || '邀请链接无效或已过期'
    pageState.value = 'invalid'
  }
}

async function handleVerifyCode() {
  if (!accessCode.value.trim()) {
    verifyError.value = '请输入访问码'
    return
  }
  verifying.value = true
  verifyError.value = ''
  await doVerify(accessCode.value.trim())
  verifying.value = false
}

async function doVerify(code: string) {
  try {
    const res = await verifyAccessCode(inviteCode, { accessCode: code })
    const data = res.data.data
    localStorage.setItem(anonTokenKey, data.token)
    setActiveAnonToken(data.token)
    albumName.value = data.albumName || albumName.value
    await loadMyUploads()
    pageState.value = 'authenticated'
  } catch (err: any) {
    const msg = err.response?.data?.msg || '验证失败'
    if (pageState.value === 'needCode') {
      verifyError.value = msg
    } else {
      errorMsg.value = msg
      pageState.value = 'invalid'
    }
  }
}

async function loadMyUploads() {
  const res = await getMyUploads()
  uploads.value = res.data.data || []
}

function onImageUploaded() {
  loadMyUploads()
}

async function handleSubmitText() {
  if (!textContent.value.trim()) return
  submittingText.value = true
  textMsg.value = ''
  try {
    await uploadText({ content: textContent.value.trim() })
    textContent.value = ''
    textMsg.value = '留言提交成功'
    textMsgType.value = 'success'
    await loadMyUploads()
  } catch (err: any) {
    textMsg.value = err.response?.data?.msg || '提交失败'
    textMsgType.value = 'error'
  } finally {
    submittingText.value = false
    setTimeout(() => { textMsg.value = '' }, 3000)
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
  <div v-else class="space-y-6">
    <!-- Header -->
    <div class="text-center">
      <h1 class="text-xl sm:text-2xl font-bold">{{ albumName }}</h1>
      <p v-if="albumDes" class="text-sm text-muted-foreground mt-1">{{ albumDes }}</p>
    </div>

    <!-- Image upload -->
    <Card>
      <CardHeader>
        <CardTitle class="text-base">上传图片</CardTitle>
      </CardHeader>
      <CardContent>
        <ImageUploader @uploaded="onImageUploaded" />
      </CardContent>
    </Card>

    <!-- Text message -->
    <Card>
      <CardHeader>
        <CardTitle class="text-base">留言</CardTitle>
      </CardHeader>
      <CardContent>
        <form @submit.prevent="handleSubmitText" class="space-y-3">
          <textarea
            v-model="textContent"
            placeholder="写下你想说的话..."
            rows="3"
            class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50 resize-none"
          />
          <div class="flex items-center justify-between">
            <p v-if="textMsg" class="text-sm" :class="textMsgType === 'success' ? 'text-green-600' : 'text-destructive'">{{ textMsg }}</p>
            <span v-else />
            <Button type="submit" size="sm" :disabled="submittingText || !textContent.trim()">
              {{ submittingText ? '提交中...' : '提交留言' }}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>

    <!-- Uploaded media list -->
    <Card v-if="uploads.length > 0">
      <CardHeader>
        <CardTitle class="text-base">我的上传 ({{ uploads.length }})</CardTitle>
      </CardHeader>
      <CardContent>
        <div class="grid grid-cols-2 sm:grid-cols-3 gap-3">
          <template v-for="item in uploads" :key="item.id">
            <!-- Image -->
            <div v-if="item.type === 2" class="aspect-square rounded-md overflow-hidden border">
              <img :src="item.content" alt="已上传图片" class="h-full w-full object-cover" />
            </div>
            <!-- Text -->
            <div v-else class="col-span-2 sm:col-span-3 rounded-md border p-3">
              <p class="text-sm text-muted-foreground whitespace-pre-wrap">{{ item.content }}</p>
            </div>
          </template>
        </div>
      </CardContent>
    </Card>
  </div>
</template>
