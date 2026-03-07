<script setup lang="ts">
import { cn } from '@/lib/utils'
import { computed, useAttrs } from 'vue'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  class?: string
  modelValue?: string | number
}>(), {
  class: '',
  modelValue: '',
})

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
}>()

const attrs = useAttrs()

const delegatedProps = computed(() => {
  const { class: _, ...delegated } = props
  return delegated
})
</script>

<template>
  <input
    v-bind="{ ...attrs, ...delegatedProps }"
    :class="cn('flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background file:border-0 file:bg-transparent file:text-sm file:font-medium placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50', props.class)"
    :value="modelValue"
    @input="emit('update:modelValue', ($event.target as HTMLInputElement).value)"
  />
</template>
