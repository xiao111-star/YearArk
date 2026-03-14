import type { ImageSlotValue } from '@/types/editor'

/**
 * HTML 特殊字符转义（仅用于 text slot）
 */
export function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#x27;')
}

/**
 * 类型守卫：判断值是否为 ImageSlotValue
 */
export function isImageSlotValue(value: unknown): value is ImageSlotValue {
  return (
    typeof value === 'object' &&
    value !== null &&
    'url' in value &&
    typeof (value as ImageSlotValue).url === 'string'
  )
}

/**
 * 构建 focus_point + scale 的 CSS style 字符串
 */
function buildFocusStyle(slot: ImageSlotValue): string {
  const fx = ((slot.focus_x ?? 0.5) * 100).toFixed(1)
  const fy = ((slot.focus_y ?? 0.5) * 100).toFixed(1)
  const scale = (slot.scale ?? 1.0).toFixed(2)
  return `object-fit:cover;object-position:${fx}% ${fy}%;transform:scale(${scale});transform-origin:${fx}% ${fy}%`
}

/**
 * 在 HTML 中找到 src 等于指定 URL 的 img 标签，追加或合并 style 属性
 */
function injectImgStyle(html: string, url: string, style: string): string {
  // Escape special regex chars in URL
  const escapedUrl = url.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const imgPattern = new RegExp(`(<img\\b[^>]*src=["']${escapedUrl}["'][^>]*)(>)`, 'g')
  return html.replace(imgPattern, (_, imgTag: string, close: string) => {
    if (imgTag.includes('style=')) {
      // Append to existing style
      imgTag = imgTag.replace(/(style=["'])([^"']*)/, `$1$2;${style}`)
    } else {
      imgTag = `${imgTag} style="${style}"`
    }
    return imgTag + close
  })
}

/**
 * 将 templateHtml 中的 {{slot_id}} 占位符替换为 dataJson 中的值。
 *
 * 与后端 TemplateRenderServiceImpl 保持一致：
 * - image slot：模板里是 <img src="{{image_xxx}}" .../>，先把占位符替换成 URL，再注入 focus/scale style
 * - text slot：直接替换占位符文本（不转义，与后端一致）
 * - 未定义的占位符：替换为空字符串
 */
export function renderPage(
  templateHtml: string,
  data: Record<string, string | ImageSlotValue>
): string {
  if (!templateHtml) return ''

  let result = templateHtml

  for (const [key, value] of Object.entries(data)) {
    const placeholder = `{{${key}}}`
    if (isImageSlotValue(value)) {
      if (value.url) {
        // Step 1: replace placeholder with actual URL (same as backend)
        result = result.replaceAll(placeholder, value.url)
        // Step 2: inject focus/scale style into the <img> tag
        result = injectImgStyle(result, value.url, buildFocusStyle(value))
      } else {
        result = result.replaceAll(placeholder, '')
      }
    } else {
      // text slot: plain replacement (no escaping, consistent with backend)
      const text = value != null ? String(value) : ''
      result = result.replaceAll(placeholder, text)
    }
  }

  // Remove any remaining unmatched placeholders
  result = result.replace(/\{\{[^}]+\}\}/g, '')

  return result
}
