import request from '@/utils/request'

/** 创建纪念册 */
export function createAlbum(data: { name: string; des?: string; templateId?: number }) {
  return request.post('/api/user/album', data)
}

/** 我的纪念册列表 */
export function listAlbums() {
  return request.get('/api/user/album/list')
}

/** 纪念册详情 */
export function getAlbumDetail(id: number) {
  return request.get(`/api/user/album/${id}`)
}

/** 更新纪念册 */
export function updateAlbum(data: { id: number; name?: string; des?: string; templateId?: number }) {
  return request.post('/api/user/album/update', data)
}

/** 删除纪念册 */
export function deleteAlbum(id: number) {
  return request.delete(`/api/user/album/${id}`)
}

/** 生成纪念册 */
export function generateAlbum(id: number) {
  return request.post(`/api/user/album/${id}/generate`)
}

/** 预览纪念册 */
export function previewAlbum(id: number) {
  return request.get(`/api/user/album/${id}/preview`)
}
