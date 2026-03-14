/**
 * Clamp image adjustment values to valid ranges:
 * - focus_x: [0.0, 1.0]
 * - focus_y: [0.0, 1.0]
 * - scale: [0.5, 3.0]
 */
export function clampImageValue(value: {
  focus_x: number
  focus_y: number
  scale: number
}): { focus_x: number; focus_y: number; scale: number } {
  return {
    focus_x: Math.min(1.0, Math.max(0.0, value.focus_x)),
    focus_y: Math.min(1.0, Math.max(0.0, value.focus_y)),
    scale: Math.min(3.0, Math.max(0.5, value.scale)),
  }
}
