<script setup lang="ts">
import { ref, computed } from 'vue'
import FocusPointPicker from './FocusPointPicker.vue'
import type { UnusedMedia } from './MediaPanel.vue'

interface SlotDef {
  id: string
  type: 'image' | 'text'
  label?: string
  required?: boolean
  maxLength?: number
}

interface ImageSlotValue {
  url: string
  focus_x: number
  focus_y: number
  scale: number
}

export interface EditablePage {
  pageId: number
  sort: number
  html: string
  data: Record<string, unknown>
  schemaContent: string
}

const props = defineProps<{
  page: EditablePage
  unusedMedia: UnusedMedia[]
}>()

const emit = defineEmits<{
  'update': [data: Record<string, unknown>]
}>()

// Local mutable copy of data
const localData = ref<Record<string, unknown>>(JSON.parse(JSON.stringify(props.page.data ?? {})))

// Parse slots from schema
const slots = computed<SlotDef[]>(() => {
  try {
    const schema = JSON.parse(props.page.schemaContent ?? '{}')
    return schema.slots ?? []
  } catch { return [] }
})

const imageSlots = computed(() => slots.value.filter(s => s.type === 'image'))
const textSlots = computed(() => slots.value.filter(s => s.type === 'text'))

// Active focus picker slot
const activeFocusSlot = ref<string | null>(null)

// Drag state for image swap
const dragSourceSlot = ref<string | null>(null)

function getImageValue(slotId: string): ImageSlotValue {
  const v = localData.value[slotId]
  if (v && typeof v === 'object' && 'url' in (v as object)) return v as ImageSlotValue
  return { url: typeof v === 'string' ? v : '', focus_x: 0.5, focus_y: 0.5, scale: 1.0 }
}

function setImageValue(slotId: string, val: ImageSlotValue) {
  localData.value = { ...localData.value, [slotId]: val }
  emit('update', localData.value)
}

function getTextValue(slotId: string): string {
  return String(localData.value[slotId] ?? '')
}

function setTextValue(slotId: string, val: string) {
  localData.value = { ...localData.value, [slotId]: val }
  emit('update', localData.value)
}

// Image drag-swap between slots
function onSlotDragStart(slotId: string) { dragSourceSlot.value = slotId }

function onSlotDrop(targetSlotId: string) {
  if (!dragSourceSlot.value || dragSourceSlot.value === targetSlotId) return
  const a = getImageValue(dragSourceSlot.value)
  const b = getImageValue(targetSlotId)
  localData.value = { ...localData.value, [dragSourceSlot.value]: b, [targetSlotId]: a }
  emit('update', localData.value)
  dragSourceSlot.value = null
}

// Drop from MediaPanel
function onMediaDrop(slotId: string, media: UnusedMedia) {
  setImageValue(slotId, { url: media.content, focus_x: 0.5, focus_y: 0.5, scale: 1.0 })
}

function onExternalDragOver(e: DragEvent) { e.preventDefault() }
</script>

<template>
  <div class="space-y-4">
    <!-- Image slots -->
    <div v-if="imageSlots.length > 0" class="space-y-4">
      <div v-for="slot in imageSlots" :key="slot.id" class="border rounded-lg p-3 space-y-2">
        <div class="flex items-center justify-between">
          <span class="text-sm font-medium">{{ slot.label ?? slot.id }}</span>
          <button
            class="text-xs text-primary underline"
            @click="activeFocusSlot = activeFocusSlot === slot.id ? null : slot.id"
          >
            {{ activeFocusSlot === slot.id ? '收起' : '调整焦点/缩放' }}
          </button>
        </div>

        <!-- Image preview + drop zone -->
        <div
          class="relative w-full h-32 rounded overflow-hidden border-2 border-dashed border-muted-foreground/30 cursor-grab"
          draggable="true"
          @dragstart="onSlotDragStart(slot.id)"
          @dragover.prevent="onExternalDragOver"
          @drop.prevent="(e) => {
            if (dragSourceSlot) onSlotDrop(slot.id)
          }"
        >
          <img
            v-if="getImageValue(slot.id).url"
            :src="getImageValue(slot.id).url"
            class="w-full h-full object-cover"
            :style="{
              objectPosition: `${getImageValue(slot.id).focus_x * 100}% ${getImageValue(slot.id).focus_y * 100}%`,
              transform: `scale(${getImageValue(slot.id).scale})`,
              transformOrigin: `${getImageValue(slot.id).focus_x * 100}% ${getImageValue(slot.id).focus_y * 100}%`,
            }"
          />
          <div v-else class="flex items-center justify-center h-full text-xs text-muted-foreground">
            拖入图片或从素材库选择
          </div>
        </div>

        <!-- FocusPointPicker -->
        <FocusPointPicker
          v-if="activeFocusSlot === slot.id"
          :model-value="getImageValue(slot.id)"
          @update:model-value="setImageValue(slot.id, $event)"
        />
      </div>
    </div>

    <!-- Text slots -->
    <div v-if="textSlots.length > 0" class="space-y-3">
      <div v-for="slot in textSlots" :key="slot.id" class="space-y-1">
        <label class="text-sm font-medium">{{ slot.label ?? slot.id }}</label>
        <textarea
          class="w-full rounded-md border px-3 py-2 text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary"
          rows="2"
          :maxlength="slot.maxLength"
          :value="getTextValue(slot.id)"
          @input="setTextValue(slot.id, ($event.target as HTMLTextAreaElement).value)"
        />
        <p v-if="slot.maxLength" class="text-xs text-muted-foreground text-right">
          {{ getTextValue(slot.id).length }} / {{ slot.maxLength }}
        </p>
      </div>
    </div>
  </div>
</template>
