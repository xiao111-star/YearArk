/** 图片 slot 值 */
export interface ImageSlotValue {
  url: string
  focus_x: number  // 0.0 ~ 1.0，默认 0.5
  focus_y: number  // 0.0 ~ 1.0，默认 0.5
  scale: number    // 0.5 ~ 3.0，默认 1.0
}

/** 文本 slot 值 = string */
export type TextSlotValue = string

/** slot 值联合类型 */
export type SlotValue = TextSlotValue | ImageSlotValue

/** Schema 中的 slot 定义 */
export interface SlotDef {
  id: string          // e.g. "image_1", "text_1"
  type: 'image' | 'text'
  label: string
  required: boolean
  maxLength?: number  // text only
  width?: number      // image only
  height?: number     // image only
  default?: string
}

/** 单页编辑数据（对应后端 EditablePageVo） */
export interface PageData {
  pageId: number
  sort: number
  html: string                              // 后端渲染的 HTML（向后兼容）
  templateHtml: string                      // 模板原始 HTML（含占位符）
  data: Record<string, SlotValue>           // DataJson
  schemaContent: string                     // Schema JSON 字符串
}
