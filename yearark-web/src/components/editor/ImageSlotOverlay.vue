<script setup lang="ts">
import { Camera } from 'lucide-vue-next'

defineProps<{
  slotId: string
  position: { x: number; y: number; width: number; height: number }
}>()

const emit = defineEmits<{
  select: [slotId: string]
}>()

function handleClick(slotId: string) {
  emit('select', slotId)
}
</script>

<template>
  <div
    class="absolute cursor-pointer group"
    :style="{
      left: position.x + 'px',
      top: position.y + 'px',
      width: position.width + 'px',
      height: position.height + 'px',
    }"
    @click="handleClick(slotId)"
  >
    <div
      class="w-full h-full bg-black/0 group-hover:bg-black/30 transition-colors flex items-center justify-center rounded"
    >
      <Camera
        class="w-8 h-8 text-white opacity-0 group-hover:opacity-80 transition-opacity"
      />
    </div>
  </div>
</template>
