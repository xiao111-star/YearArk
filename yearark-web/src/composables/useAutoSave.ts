import { ref } from 'vue'
import { updatePageData } from '@/api/album'
import { useToast } from '@/components/ui/toast/use-toast'

export function useAutoSave() {
  const saving = ref(false)
  const { toast } = useToast()

  async function save(pageId: number, data: Record<string, unknown>) {
    saving.value = true
    try {
      const res = await updatePageData(pageId, data)
      toast({ description: '已保存', duration: 1500 })
      return res
    } catch (error: any) {
      const msg =
        error?.response?.data?.msg ||
        error?.response?.data?.message ||
        error?.message ||
        '保存失败'
      toast({ description: msg, variant: 'destructive', duration: 3000 })
      throw error // re-throw so caller knows save failed; user input is preserved
    } finally {
      saving.value = false
    }
  }

  return { saving, save }
}
