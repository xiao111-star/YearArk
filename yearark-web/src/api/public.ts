import request from '@/utils/request'

/** 公开纪念册列表（首页展示，无需登录） */
export function listPublicAlbums() {
  return request.get('/api/public/album/list')
}

/** 公开查看纪念册（分享链接，无需登录） */
export function getPublicAlbumView(id: number) {
  return request.get(`/api/public/album/${id}/view`)
}
