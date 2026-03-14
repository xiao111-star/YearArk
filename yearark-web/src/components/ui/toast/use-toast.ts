import { ref, computed } from 'vue'

export interface Toast {
  id: string
  description: string
  variant?: 'default' | 'destructive'
  duration?: number
}

const toasts = ref<Toast[]>([])
let count = 0

function genId() {
  count = (count + 1) % Number.MAX_SAFE_INTEGER
  return count.toString()
}

function addToast(props: Omit<Toast, 'id'>) {
  const id = genId()
  const duration = props.duration ?? 3000
  const toast: Toast = { ...props, id }
  toasts.value.push(toast)

  if (duration > 0) {
    setTimeout(() => {
      removeToast(id)
    }, duration)
  }

  return id
}

function removeToast(id: string) {
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

export function useToast() {
  return {
    toasts: computed(() => toasts.value),
    toast: addToast,
    dismiss: removeToast,
  }
}
