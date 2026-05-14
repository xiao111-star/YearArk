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

/** 获取生成状态 */
export function getAlbumStatus(id: number) {
  return request.get(`/api/user/album/${id}/status`)
}

/** 获取编辑数据（每页 Data JSON + schemaContent + 渲染 HTML） */
export function getAlbumEditData(id: number) {
  return request.get(`/api/user/album/${id}/edit-data`)
}

/** 更新单页 Data JSON */
export function updatePageData(pageId: number, data: Record<string, unknown>) {
  return request.put(`/api/user/album/page/${pageId}`, { data })
}

/** 批量更新多页 Data JSON */
export function batchUpdatePages(albumId: number, updates: { pageId: number; dataMap: Record<string, unknown> }[]) {
  return request.put(`/api/user/album/${albumId}/pages`, updates)
}

/** 获取未使用的图片素材 */
export function getUnusedMedia(albumId: number) {
  return request.get(`/api/user/album/${albumId}/unused-media`)
}

/** 登录用户上传图片到纪念册素材库 */
export function uploadAlbumMedia(albumId: number, file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/api/user/album/${albumId}/media/upload`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 切换纪念册公开状态（放到/取消放到首页） */
export function toggleAlbumPublic(id: number) {
  return request.post(`/api/user/album/${id}/toggle-public`)
}

/** 发布纪念册（从草稿改为发布状态） */
export function publishAlbum(id: number) {
  return request.post(`/api/user/album/${id}/publish`)
}
