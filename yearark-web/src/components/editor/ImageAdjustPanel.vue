<script setup lang="ts">
import { ref, computed } from 'vue'
import { Check, X, ZoomIn } from 'lucide-vue-next'
import { Button } from '@/components/ui/button'
import type { ImageSlotValue } from '@/types/editor'
import { clampImageValue } from '@/utils/imageClamp'

const props = defineProps<{
  imageUrl: string
  initialValue: ImageSlotValue
}>()

const emit = defineEmits<{
  confirm: [value: ImageSlotValue]
  cancel: []
}>()

// Local editable state, initialized from props
const focusX = ref(props.initialValue.focus_x)
const focusY = ref(props.initialValue.focus_y)
const scale = ref(props.initialValue.scale)
const dragging = ref(false)

// Computed styles for the preview image
const imageStyle = computed(() => ({
  objectFit: 'cover' as const,
  objectPosition: `${focusX.value * 100}% ${focusY.value * 100}%`,
  transform: `scale(${scale.value})`,
  transformOrigin: `${focusX.value * 100}% ${focusY.value * 100}%`,
}))

// Computed position for the focus indicator dot
const dotStyle = computed(() => ({
  left: `${focusX.value * 100}%`,
  top: `${focusY.value * 100}%`,
}))

/** Update focus from a mouse/touch position relative to the preview container */
function updateFocusFromEvent(clientX: number, clientY: number, container: HTMLElement) {
  const rect = container.getBoundingClientRect()
  const x = (clientX - rect.left) / rect.width
  const y = (clientY - rect.top) / rect.height
  const clamped = clampImageValue({ focus_x: x, focus_y: y, scale: scale.value })
  focusX.value = clamped.focus_x
  focusY.value = clamped.focus_y
}

function onPointerDown(e: PointerEvent) {
  const container = e.currentTarget as HTMLElement
  container.setPointerCapture(e.pointerId)
  dragging.value = true
  updateFocusFromEvent(e.clientX, e.clientY, container)
}

function onPointerMove(e: PointerEvent) {
  if (!dragging.value) return
  const container = e.currentTarget as HTMLElement
  updateFocusFromEvent(e.clientX, e.clientY, container)
}

function onPointerUp() {
  dragging.value = false
}

function onScaleInput(e: Event) {
  const input = e.target as HTMLInputElement
  const raw = parseFloat(input.value)
  const clamped = clampImageValue({ focus_x: focusX.value, focus_y: focusY.value, scale: raw })
  scale.value = clamped.scale
}

function confirm() {
  const clamped = clampImageValue({ focus_x: focusX.value, focus_y: focusY.value, scale: scale.value })
  emit('confirm', {
    url: props.imageUrl,
    focus_x: clamped.focus_x,
    focus_y: clamped.focus_y,
    scale: clamped.scale,
  })
}

function cancel() {
  emit('cancel')
}

function onBackdropClick(e: MouseEvent) {
  if (e.target === e.currentTarget) cancel()
}
</script>

<template>
  <Teleport to="body">
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      @click="onBackdropClick"
    >
      <div class="bg-background rounded-lg shadow-xl w-full max-w-lg flex flex-col mx-4">
        <!-- Header -->
        <div class="flex items-center justify-between px-5 py-4 border-b">
          <h3 class="text-lg font-medium">调整图片</h3>
          <button class="text-muted-foreground hover:text-foreground" @click="cancel">
            <X class="w-5 h-5" />
          </button>
        </div>

        <!-- Image preview area with drag support -->
        <div class="px-5 pt-4">
          <p class="text-xs text-muted-foreground mb-2">拖拽图片调整焦点位置</p>
          <div
            class="relative w-full aspect-[4/3] rounded-lg overflow-hidden border bg-muted cursor-crosshair select-none"
            @pointerdown="onPointerDown"
            @pointermove="onPointerMove"
            @pointerup="onPointerUp"
            @pointercancel="onPointerUp"
          >
            <img
              :src="imageUrl"
              alt="预览"
              class="w-full h-full pointer-events-none"
              :style="imageStyle"
              draggable="false"
            />
            <!-- Focus point indicator -->
            <div
              class="absolute w-5 h-5 -translate-x-1/2 -translate-y-1/2 pointer-events-none"
              :style="dotStyle"
            >
              <div class="w-full h-full rounded-full border-2 border-white shadow-md bg-primary/40" />
              <div class="absolute inset-0 flex items-center justify-center">
                <div class="w-1.5 h-1.5 rounded-full bg-white shadow" />
              </div>
            </div>
          </div>
        </div>

        <!-- Scale slider -->
        <div class="px-5 py-4">
          <div class="flex items-center gap-3">
            <ZoomIn class="w-4 h-4 text-muted-foreground shrink-0" />
            <input
              type="range"
              min="0.5"
              max="3.0"
              step="0.1"
              :value="scale"
              class="flex-1 h-2 bg-muted rounded-lg appearance-none cursor-pointer accent-primary"
              @input="onScaleInput"
            />
            <span class="text-sm text-muted-foreground w-10 text-right tabular-nums">
              {{ scale.toFixed(1) }}
            </span>
          </div>
        </div>

        <!-- Action buttons -->
        <div class="flex items-center justify-end gap-3 px-5 py-4 border-t">
          <Button variant="outline" @click="cancel">
            <X class="w-4 h-4 mr-1.5" />
            取消
          </Button>
          <Button @click="confirm">
            <Check class="w-4 h-4 mr-1.5" />
            确认
          </Button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
