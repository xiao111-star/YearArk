import { ref, type Ref } from 'vue'
import { getAlbumEditData, getAlbumDetail } from '@/api/album'
import { useAutoSave } from '@/composables/useAutoSave'
import type { PageData, SlotValue, ImageSlotValue } from '@/types/editor'

/**
 * 判断一个值是否为 ImageSlotValue
 */
function isImageSlotValue(val: unknown): val is ImageSlotValue {
  return (
    typeof val === 'object' &&
    val !== null &&
    'url' in val &&
    typeof (val as ImageSlotValue).url === 'string'
  )
}

/**
 * 将后端返回的 data map 转换为前端 SlotValue 类型
 * - string → TextSlotValue
 * - object with url → ImageSlotValue
 */
function normalizeSlotData(raw: Record<string, unknown>): Record<string, SlotValue> {
  const result: Record<string, SlotValue> = {}
  for (const [key, val] of Object.entries(raw)) {
    if (typeof val === 'string') {
      result[key] = val
    } else if (isImageSlotValue(val)) {
      result[key] = {
        url: val.url,
        focus_x: val.focus_x ?? 0.5,
        focus_y: val.focus_y ?? 0.5,
        scale: val.scale ?? 1.0,
      }
    }
  }
  return result
}

export function useAlbumEditor(albumId: Ref<number>) {
  const pages = ref<PageData[]>([])
  const loading = ref(false)
  const albumName = ref('')
  const autoSave = useAutoSave()

  async function loadEditData(): Promise<void> {
    loading.value = true
    try {
      const [editRes, detailRes] = await Promise.all([
        getAlbumEditData(albumId.value),
        getAlbumDetail(albumId.value),
      ])

      const editList: any[] = editRes.data?.data ?? editRes.data ?? []
      pages.value = editList.map((item: any) => ({
        pageId: item.pageId,
        sort: item.sort,
        html: item.html ?? '',
        templateHtml: item.templateHtml ?? '',
        data: normalizeSlotData(item.data ?? {}),
        schemaContent: item.schemaContent ?? '',
      }))

      const detail = detailRes.data?.data ?? detailRes.data ?? {}
      albumName.value = detail.name ?? ''
    } finally {
      loading.value = false
    }
  }

  function updateSlotValue(pageId: number, slotId: string, value: SlotValue, doSave: boolean = true): void {
    const page = pages.value.find((p) => p.pageId === pageId)
    if (!page) return

    page.data[slotId] = value

    if (doSave) {
      savePageData(pageId)
    }
  }

  function savePageData(pageId: number) {
    const page = pages.value.find((p) => p.pageId === pageId)
    if (!page) return
    const fullData: Record<string, unknown> = { ...page.data }
    autoSave.save(pageId, fullData)
  }

  function getPageData(pageId: number): PageData | undefined {
    return pages.value.find((p) => p.pageId === pageId)
  }

  return { pages, loading, albumName, loadEditData, updateSlotValue, savePageData, getPageData }
}
