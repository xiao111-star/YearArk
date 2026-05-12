<script setup lang="ts">
import { ref } from 'vue'
import { Button } from '@/components/ui/button'

export interface Invite {
  id: number
  albumId: number
  inviteCode: string
  accessCode: string
  status: number // 0=可用, 1=停用（与 sys_normal_disable 字典一致）
  expireAt: string
}

const props = defineProps<{
  invite: Invite
}>()

const emit = defineEmits<{
  disable: [id: number]
}>()

const copied = ref(false)

const shareUrl = (() => {
  const loc = window.location
  let host = loc.host
  if (loc.hostname === 'localhost' || loc.hostname === '127.0.0.1') {
    host = 'yearark.top'
  }
  return `${loc.protocol}//${host}/share/${props.invite.inviteCode}`
})()

const isExpired = props.invite.expireAt ? new Date(props.invite.expireAt) < new Date() : false
const isActive = props.invite.status === 0 && !isExpired

function formatTime(dateStr: string) {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** 兼容非 HTTPS 环境的复制方法 */
function fallbackCopy(text: string): boolean {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.cssText = 'position:fixed;left:-9999px;top:-9999px;opacity:0;'
  document.body.appendChild(textarea)
  textarea.select()
  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch { /* ignore */ }
  document.body.removeChild(textarea)
  return ok
}

async function copyLink() {
  let success = false
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(shareUrl)
      success = true
    }
  } catch { /* Clipboard API 不可用（非 HTTPS） */ }

  if (!success) {
    success = fallbackCopy(shareUrl)
  }

  if (success) {
    copied.value = true
    setTimeout(() => (copied.value = false), 2000)
  }
}
</script>

<template>
  <div class="flex items-center justify-between gap-3 rounded-lg border p-3">
    <div class="min-w-0 flex-1">
      <div class="flex items-center gap-2 mb-1">
        <code class="text-sm font-mono">{{ props.invite.inviteCode }}</code>
        <span
          :class="[
            'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
            isActive ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500',
          ]"
        >
          {{ isExpired ? '已过期' : props.invite.status === 0 ? '可用' : '已禁用' }}
        </span>
      </div>
      <p class="text-xs text-muted-foreground truncate">{{ shareUrl }}</p>
      <p class="text-xs text-muted-foreground mt-0.5">
        过期时间：{{ formatTime(props.invite.expireAt) }} · 访问码：{{ props.invite.accessCode }}
      </p>
    </div>
    <div class="flex items-center gap-2 shrink-0">
      <Button size="sm" variant="outline" @click="copyLink">
        {{ copied ? '已复制' : '复制链接' }}
      </Button>
      <Button
        v-if="isActive"
        size="sm"
        variant="destructive"
        @click="emit('disable', props.invite.id)"
      >
        禁用
      </Button>
    </div>
  </div>
</template>
