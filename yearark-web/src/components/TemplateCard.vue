<script setup lang="ts">
import { Card, CardContent } from '@/components/ui/card'

export interface Template {
  id: number
  name: string
  previewImage: string
  des: string
}

const props = defineProps<{
  template: Template
  selected: boolean
}>()

const emit = defineEmits<{
  select: [id: number]
}>()
</script>

<template>
  <Card
    class="cursor-pointer transition-all hover:shadow-md"
    :class="[
      props.selected
        ? 'ring-2 ring-primary border-primary shadow-md'
        : 'hover:border-muted-foreground/30',
    ]"
    @click="emit('select', props.template.id)"
  >
    <div class="relative aspect-[3/4] overflow-hidden rounded-t-lg bg-muted">
      <img
        v-if="props.template.previewImage"
        :src="props.template.previewImage"
        :alt="props.template.name"
        class="h-full w-full object-cover"
      />
      <div v-else class="flex h-full items-center justify-center text-muted-foreground text-sm">
        暂无预览
      </div>
      <div
        v-if="props.selected"
        class="absolute inset-0 bg-primary/10 flex items-center justify-center"
      >
        <span class="rounded-full bg-primary text-primary-foreground px-3 py-1 text-xs font-medium">
          已选择
        </span>
      </div>
    </div>
    <CardContent class="p-3">
      <h3 class="text-sm font-medium truncate">{{ props.template.name }}</h3>
      <p v-if="props.template.des" class="text-xs text-muted-foreground mt-1 line-clamp-2">
        {{ props.template.des }}
      </p>
    </CardContent>
  </Card>
</template>
