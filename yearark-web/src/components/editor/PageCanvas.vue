<script setup lang="ts">
import { computed, ref, onMounted, nextTick, watch } from 'vue'
import { renderPage } from '@/utils/albumRenderer'
import type { PageData, SlotDef } from '@/types/editor'
import TextSlotOverlay from './TextSlotOverlay.vue'
import ImageSlotOverlay from './ImageSlotOverlay.vue'

const props = defineProps<{
  pageData: PageData
}>()

const emit = defineEmits<{
  'update:textSlot': [pageId: number, slotId: string, newText: string]
  'selectImageSlot': [pageId: number, slotId: string]
}>()

const renderedHtml = computed(() =>
  renderPage(props.pageData.templateHtml, props.pageData.data)
)

const slots = computed<SlotDef[]>(() => {
  try {
    const schema = JSON.parse(props.pageData.schemaContent || '{}')
    return schema.slots || []
  } catch {
    return []
  }
})

const textSlots = computed(() => slots.value.filter(s => s.type === 'text'))
const imageSlots = computed(() => slots.value.filter(s => s.type === 'image'))

// Container ref for positioning overlays
const canvasRef = ref<HTMLElement | null>(null)
const slotPositions = ref<Record<string, { x: number; y: number; width: number; height: number }>>({})

function updateSlotPositions() {
  if (!canvasRef.value) return
  const positions: Record<string, { x: number; y: number; width: number; height: number }> = {}
  for (const slot of slots.value) {
    const el = canvasRef.value.querySelector(`[data-slot-id="${slot.id}"]`) as HTMLElement
    if (el) {
      const containerRect = canvasRef.value.getBoundingClientRect()
      const elRect = el.getBoundingClientRect()
      positions[slot.id] = {
        x: elRect.left - containerRect.left,
        y: elRect.top - containerRect.top,
        width: elRect.width,
        height: elRect.height,
      }
    }
  }
  slotPositions.value = positions
}

function handleTextUpdate(slotId: string, newText: string) {
  emit('update:textSlot', props.pageData.pageId, slotId, newText)
}

function handleImageClick(slotId: string) {
  emit('selectImageSlot', props.pageData.pageId, slotId)
}

onMounted(async () => {
  await nextTick()
  updateSlotPositions()
})

watch(renderedHtml, async () => {
  await nextTick()
  updateSlotPositions()
})
</script>

<template>
  <div class="relative" ref="canvasRef">
    <!-- Rendered page HTML -->
    <div v-html="renderedHtml" class="page-content" />

    <!-- Text slot overlays -->
    <TextSlotOverlay
      v-for="slot in textSlots"
      :key="slot.id"
      :slot-id="slot.id"
      :value="typeof pageData.data[slot.id] === 'string' ? (pageData.data[slot.id] as string) : ''"
      :position="slotPositions[slot.id] ?? { x: 0, y: 0, width: 0, height: 0 }"
      @update:value="handleTextUpdate"
    />

    <!-- Image slot overlays -->
    <ImageSlotOverlay
      v-for="slot in imageSlots"
      :key="slot.id"
      :slot-id="slot.id"
      :position="slotPositions[slot.id] ?? { x: 0, y: 0, width: 0, height: 0 }"
      @select="handleImageClick"
    />
  </div>
</template>
