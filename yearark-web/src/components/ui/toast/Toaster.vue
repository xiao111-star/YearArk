<script setup lang="ts">
import { useToast } from './use-toast'
import { cn } from '@/lib/utils'
import { X } from 'lucide-vue-next'

const { toasts, dismiss } = useToast()
</script>

<template>
  <Teleport to="body">
    <div class="fixed top-20 left-1/2 -translate-x-1/2 z-[100] flex flex-col gap-2 w-full max-w-sm">
      <TransitionGroup
        enter-active-class="transition-all duration-300 ease-out"
        leave-active-class="transition-all duration-200 ease-in"
        enter-from-class="opacity-0 translate-y-2"
        enter-to-class="opacity-100 translate-y-0"
        leave-from-class="opacity-100 translate-y-0"
        leave-to-class="opacity-0 translate-y-2"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          :class="cn(
            'pointer-events-auto flex items-center justify-between gap-2 rounded-md border px-4 py-3 shadow-lg',
            toast.variant === 'destructive'
              ? 'border-red-200 bg-red-50 text-red-900'
              : 'border-border bg-background text-foreground'
          )"
        >
          <p class="text-sm">{{ toast.description }}</p>
          <button
            class="shrink-0 rounded-sm opacity-70 hover:opacity-100"
            @click="dismiss(toast.id)"
          >
            <X class="h-4 w-4" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
