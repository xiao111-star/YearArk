<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'

const props = defineProps<{
  slotId: string
  value: string
  position: { x: number; y: number; width: number; height: number }
}>()

const emit = defineEmits<{
  'update:value': [slotId: string, newText: string]
}>()

const editing = ref(false)
const localText = ref(props.value)
const editableRef = ref<HTMLElement | null>(null)

// Sync from parent when not editing
watch(() => props.value, (v) => {
  if (!editing.value) {
    localText.value = v
  }
})

async function startEdit() {
  editing.value = true
  await nextTick()
  editableRef.value?.focus()
}

function finishEdit() {
  if (!editing.value) return
  editing.value = false
  const newText = localText.value
  if (newText !== props.value) {
    emit('update:value', props.slotId, newText)
  }
}

function onInput(e: Event) {
  localText.value = (e.target as HTMLElement).textContent || ''
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter') {
    e.preventDefault()
    finishEdit()
  }
}

// Strip rich text on paste — plain text only
function onPaste(e: ClipboardEvent) {
  e.preventDefault()
  const text = e.clipboardData?.getData('text/plain') || ''
  document.execCommand('insertText', false, text)
}
</script>

<template>
  <div
    class="absolute cursor-text transition-all"
    :class="editing ? 'ring-2 ring-primary rounded z-10' : 'hover:ring-1 hover:ring-primary/50 hover:rounded'"
    :style="{
      left: position.x + 'px',
      top: position.y + 'px',
      width: position.width + 'px',
      height: position.height + 'px',
    }"
    @click="startEdit"
  >
    <div
      v-if="editing"
      ref="editableRef"
      contenteditable="true"
      class="w-full h-full outline-none bg-white/80 p-1 text-sm"
      @blur="finishEdit"
      @keydown="onKeydown"
      @input="onInput"
      @paste="onPaste"
      v-text="localText"
    />
  </div>
</template>
