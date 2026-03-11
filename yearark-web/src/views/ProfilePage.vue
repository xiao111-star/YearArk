<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Camera, User, Mail, Lock, Check, Loader2 } from 'lucide-vue-next'
import { useUserStore } from '@/stores/user'
import { getUserInfo, updateProfile, uploadAvatar, changePassword } from '@/api/auth'
import { sha256 } from '@/utils/crypto'

const userStore = useUserStore()

// Profile data
const profile = ref({
  username: '',
  email: '',
  avatarUrl: '',
  createAt: '',
})
const loading = ref(false)

// Edit states
const profileForm = ref({ username: '', email: '' })
const savingProfile = ref(false)
const profileMsg = ref({ type: '', text: '' })

const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const savingPassword = ref(false)
const passwordMsg = ref({ type: '', text: '' })

const uploadingAvatar = ref(false)
const avatarInput = ref<HTMLInputElement>()

async function fetchProfile() {
  loading.value = true
  try {
    const res = await getUserInfo()
    const data = res.data?.data
    if (data) {
      profile.value = data
      profileForm.value = { username: data.username || '', email: data.email || '' }
    }
  } catch {} finally { loading.value = false }
}

async function handleSaveProfile() {
  profileMsg.value = { type: '', text: '' }
  if (!profileForm.value.username.trim()) {
    profileMsg.value = { type: 'error', text: '用户名不能为空' }
    return
  }
  savingProfile.value = true
  try {
    await updateProfile(profileForm.value)
    profile.value.username = profileForm.value.username
    profile.value.email = profileForm.value.email
    userStore.setLoginInfo(userStore.token, userStore.userId, profileForm.value.username)
    profileMsg.value = { type: 'success', text: '资料已更新' }
  } catch (err: any) {
    profileMsg.value = { type: 'error', text: err.response?.data?.msg || '更新失败' }
  } finally { savingProfile.value = false }
}

async function handleAvatarChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploadingAvatar.value = true
  try {
    const res = await uploadAvatar(file)
    const url = res.data?.data || res.data?.msg
    if (url) profile.value.avatarUrl = url
  } catch {} finally { uploadingAvatar.value = false }
}

async function handleChangePassword() {
  passwordMsg.value = { type: '', text: '' }
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    passwordMsg.value = { type: 'error', text: '请填写完整' }
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    passwordMsg.value = { type: 'error', text: '新密码至少 6 位' }
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    passwordMsg.value = { type: 'error', text: '两次密码不一致' }
    return
  }
  savingPassword.value = true
  try {
    const oldHash = await sha256(passwordForm.value.oldPassword)
    const newHash = await sha256(passwordForm.value.newPassword)
    await changePassword({ oldPasswordHash: oldHash, newPasswordHash: newHash })
    passwordMsg.value = { type: 'success', text: '密码已修改' }
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } catch (err: any) {
    passwordMsg.value = { type: 'error', text: err.response?.data?.msg || '修改失败' }
  } finally { savingPassword.value = false }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

onMounted(fetchProfile)
</script>

<template>
  <div class="max-w-3xl mx-auto space-y-8">
    <!-- Page Header -->
    <div>
      <h1 class="text-2xl font-serif font-bold tracking-tight text-foreground">个人中心</h1>
      <p class="text-muted-foreground mt-1">管理你的账号信息和安全设置</p>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="space-y-6">
      <div class="h-48 rounded-xl bg-muted animate-pulse" />
      <div class="h-64 rounded-xl bg-muted animate-pulse" />
    </div>

    <template v-else>
      <!-- Avatar & Overview Card -->
      <Card>
        <CardContent class="pt-6">
          <div class="flex flex-col sm:flex-row items-center gap-6">
            <!-- Avatar -->
            <div class="relative group">
              <div
                class="w-24 h-24 rounded-full overflow-hidden border-2 border-border bg-muted flex items-center justify-center cursor-pointer"
                @click="avatarInput?.click()"
              >
                <img
                  v-if="profile.avatarUrl"
                  :src="profile.avatarUrl"
                  alt="头像"
                  class="w-full h-full object-cover"
                />
                <User v-else class="w-10 h-10 text-muted-foreground" />
                <!-- Hover overlay -->
                <div class="absolute inset-0 rounded-full bg-black/40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity">
                  <Camera v-if="!uploadingAvatar" class="w-6 h-6 text-white" />
                  <Loader2 v-else class="w-6 h-6 text-white animate-spin" />
                </div>
              </div>
              <input
                ref="avatarInput"
                type="file"
                accept="image/*"
                class="hidden"
                @change="handleAvatarChange"
              />
            </div>
            <!-- Info -->
            <div class="text-center sm:text-left">
              <h2 class="text-xl font-serif font-bold text-foreground">{{ profile.username }}</h2>
              <p class="text-sm text-muted-foreground mt-1">{{ profile.email || '未设置邮箱' }}</p>
              <p class="text-xs text-muted-foreground mt-2">注册于 {{ formatDate(profile.createAt) }}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      <!-- Profile Edit Card -->
      <Card>
        <CardHeader>
          <CardTitle class="text-lg flex items-center gap-2">
            <User class="w-5 h-5 text-primary" />
            基本信息
          </CardTitle>
          <CardDescription>修改你的用户名和邮箱</CardDescription>
        </CardHeader>
        <CardContent>
          <form @submit.prevent="handleSaveProfile" class="space-y-4">
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="space-y-2">
                <Label for="username">用户名</Label>
                <Input id="username" v-model="profileForm.username" placeholder="请输入用户名" />
              </div>
              <div class="space-y-2">
                <Label for="email">邮箱</Label>
                <div class="relative">
                  <Mail class="absolute left-3 top-2.5 w-4 h-4 text-muted-foreground" />
                  <Input id="email" v-model="profileForm.email" type="email" placeholder="请输入邮箱" class="pl-9" />
                </div>
              </div>
            </div>
            <!-- Message -->
            <p v-if="profileMsg.text" class="text-sm" :class="profileMsg.type === 'error' ? 'text-destructive' : 'text-green-600'">
              <Check v-if="profileMsg.type === 'success'" class="w-4 h-4 inline mr-1" />
              {{ profileMsg.text }}
            </p>
            <Button type="submit" :disabled="savingProfile">
              {{ savingProfile ? '保存中...' : '保存修改' }}
            </Button>
          </form>
        </CardContent>
      </Card>

      <!-- Password Card -->
      <Card>
        <CardHeader>
          <CardTitle class="text-lg flex items-center gap-2">
            <Lock class="w-5 h-5 text-primary" />
            修改密码
          </CardTitle>
          <CardDescription>定期修改密码以保障账号安全</CardDescription>
        </CardHeader>
        <CardContent>
          <form @submit.prevent="handleChangePassword" class="space-y-4">
            <div class="space-y-2">
              <Label for="oldPassword">当前密码</Label>
              <Input id="oldPassword" v-model="passwordForm.oldPassword" type="password" placeholder="请输入当前密码" autocomplete="current-password" />
            </div>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div class="space-y-2">
                <Label for="newPassword">新密码</Label>
                <Input id="newPassword" v-model="passwordForm.newPassword" type="password" placeholder="至少 6 位" autocomplete="new-password" />
              </div>
              <div class="space-y-2">
                <Label for="confirmPassword">确认新密码</Label>
                <Input id="confirmPassword" v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" autocomplete="new-password" />
              </div>
            </div>
            <!-- Message -->
            <p v-if="passwordMsg.text" class="text-sm" :class="passwordMsg.type === 'error' ? 'text-destructive' : 'text-green-600'">
              <Check v-if="passwordMsg.type === 'success'" class="w-4 h-4 inline mr-1" />
              {{ passwordMsg.text }}
            </p>
            <Button type="submit" :disabled="savingPassword">
              {{ savingPassword ? '修改中...' : '修改密码' }}
            </Button>
          </form>
        </CardContent>
      </Card>
    </template>
  </div>
</template>
