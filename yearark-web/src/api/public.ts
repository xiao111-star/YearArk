import request from '@/utils/request'

/** 公开纪念册列表（首页展示，无需登录） */
export function listPublicAlbums() {
  return request.get('/api/public/album/list')
}
