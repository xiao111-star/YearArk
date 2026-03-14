import request from '@/utils/request'

/** 某纪念册的素材列表 */
export function listMedia(params: { albumId: number; type?: number }) {
  return request.get('/api/user/media/list', { params })
}

/** 素材统计 */
export function getMediaStats(albumId: number) {
  return request.get('/api/user/media/stats', { params: { albumId } })
}

/** 删除素材 */
export function deleteMedia(mediaId: number) {
  return request.delete(`/api/user/album/media/${mediaId}`)
}
