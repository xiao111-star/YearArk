<script setup lang="ts">
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '@/components/ui/card'
import { Button } from '@/components/ui/button'

export interface Album {
  id: number
  name: string
  des: string
  status: number // 0=草稿, 1=已发布
  createTime: string
}

const props = defineProps<{
  album: Album
}>()

const emit = defineEmits<{
  detail: [id: number]
  delete: [id: number]
}>()

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
</script>

<template>
  <Card class="flex flex-col">
    <CardHeader>
      <div class="flex items-center justify-between">
        <CardTitle class="text-lg truncate">{{ props.album.name }}</CardTitle>
        <span
          :class="[
            'inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium',
            props.album.status === 1
              ? 'bg-green-100 text-green-700'
              : 'bg-gray-100 text-gray-600',
          ]"
        >
          {{ props.album.status === 1 ? '已发布' : '草稿' }}
        </span>
      </div>
      <CardDescription class="line-clamp-2">
        {{ props.album.des || '暂无描述' }}
      </CardDescription>
    </CardHeader>
    <CardContent class="flex-1">
      <p class="text-xs text-muted-foreground">
        创建于 {{ formatDate(props.album.createTime) }}
      </p>
    </CardContent>
    <CardFooter class="flex gap-2">
      <Button size="sm" @click="emit('detail', props.album.id)">详情</Button>
      <Button size="sm" variant="destructive" @click="emit('delete', props.album.id)">删除</Button>
    </CardFooter>
  </Card>
</template>
