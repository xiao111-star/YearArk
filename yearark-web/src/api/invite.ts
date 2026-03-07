import request from '@/utils/request'

/** 生成邀请链接 */
export function createInvite(data: { albumId: number; accessCode: string; expireAt: string }) {
  return request.post('/api/user/invite', data)
}

/** 某纪念册的邀请链接列表 */
export function listInvites(albumId: number) {
  return request.get('/api/user/invite/list', { params: { albumId } })
}

/** 禁用邀请链接 */
export function disableInvite(id: number) {
  return request.post(`/api/user/invite/${id}/disable`)
}
