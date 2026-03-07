<script setup lang="ts">
import { cn } from '@/lib/utils'
import { useAttrs } from 'vue'
import { buttonVariants, type ButtonVariants } from './variants'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  class?: string
  variant?: ButtonVariants['variant']
  size?: ButtonVariants['size']
  disabled?: boolean
}>(), {
  class: '',
  variant: 'default',
  size: 'default',
  disabled: false,
})

const attrs = useAttrs()
</script>

<template>
  <button
    v-bind="attrs"
    :class="cn(buttonVariants({ variant, size }), props.class)"
    :disabled="disabled"
  >
    <slot />
  </button>
</template>
