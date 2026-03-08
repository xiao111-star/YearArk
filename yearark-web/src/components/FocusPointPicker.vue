<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface ImageSlotValue {
  url: string
  focus_x: number
  focus_y: number
  scale: number
}

const props = defineProps<{ modelValue: ImageSlotValue }>()
const emit = defineEmits<{ 'update:modelValue': [v: ImageSlotValue] }>()

const pickerRef = ref<HTMLDivElement | null>(null)
const dragging = ref(false)

const previewStyle = computed(() => ({
  backgroundImage: `url(${props.modelValue.url})`,
  backgroundSize: 'cover',
  backgroundPosition: `${props.modelValue.focus_x * 100}% ${props.modelValue.focus_y * 100}%`,
  transform: `scale(${props.modelValue.scale})`,
  transformOrigin: `${props.modelValue.focus_x * 100}% ${props.modelValue.focus_y * 100}%`,
}))

const crosshairStyle = computed(() => ({
  left: `${props.modelValue.focus_x * 100}%`,
  top: `${props.modelValue.focus_y * 100}%`,
}))

function onMouseDown(e: MouseEvent) {
  dragging.value = true
  updateFocus(e)
}

function onMouseMove(e: MouseEvent) {
  if (!dragging.value) return
  updateFocus(e)
}

function onMouseUp() { dragging.value = false }

function updateFocus(e: MouseEvent) {
  const el = pickerRef.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const x = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width))
  const y = Math.min(1, Math.max(0, (e.clientY - rect.top) / rect.height))
  emit('update:modelValue', { ...props.modelValue, focus_x: x, focus_y: y })
}

function onWheel(e: WheelEvent) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.05 : 0.05
  const scale = Math.min(3, Math.max(0.5, props.modelValue.scale + delta))
  emit('update:modelValue', { ...props.modelValue, scale: Math.round(scale * 100) / 100 })
}
</script>

<template>
  <div class="space-y-3">
    <!-- Preview + crosshair picker -->
    <div
      ref="pickerRef"
      class="relative w-full h-48 overflow-hidden rounded-lg cursor-crosshair select-none border"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseUp"
      @wheel.prevent="onWheel"
    >
      <div class="absolute inset-0" :style="previewStyle" />
      <!-- crosshair -->
      <div
        class="absolute w-5 h-5 -translate-x-1/2 -translate-y-1/2 pointer-events-none"
        :style="crosshairStyle"
      >
        <div class="absolute inset-0 flex items-center justify-center">
          <div class="w-px h-full bg-white opacity-80" />
        </div>
        <div class="absolute inset-0 flex items-center justify-center">
          <div class="h-px w-full bg-white opacity-80" />
        </div>
        <div class="absolute inset-0 rounded-full border-2 border-white opacity-80" />
      </div>
    </div>

    <!-- Scale slider -->
    <div class="flex items-center gap-3">
      <span class="text-xs text-muted-foreground w-10">缩放</span>
      <input
        type="range"
        min="0.5"
        max="3"
        step="0.05"
        class="flex-1"
        :value="modelValue.scale"
        @input="emit('update:modelValue', { ...modelValue, scale: Number(($event.target as HTMLInputElement).value) })"
      />
      <span class="text-xs text-muted-foreground w-10 text-right">{{ modelValue.scale.toFixed(2) }}x</span>
    </div>
  </div>
</template>
