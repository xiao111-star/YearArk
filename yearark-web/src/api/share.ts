import request from '@/utils/request'

/** 验证邀请码，返回纪念册信息 */
export function getShareInfo(inviteCode: string) {
  return request.get(`/api/anonUser/invite/${inviteCode}`)
}

/** 验证访问码 */
export function verifyAccessCode(inviteCode: string, data: { accessCode: string }) {
  return request.post(`/api/anonUser/invite/${inviteCode}/verify`, data)
}

/** 匿名上传图片 */
export function uploadImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/anonUser/invite/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 匿名上传文字 */
export function uploadText(data: { content: string }) {
  return request.post('/api/anonUser/invite/upload/text', data)
}

/** 匿名用户已上传的素材列表 */
export function getMyUploads() {
  return request.get('/api/anonUser/invite/my-uploads')
}

/** 匿名用户删除自己上传的素材 */
export function deleteMyUpload(mediaId: number) {
  return request.delete(`/api/anonUser/invite/media/${mediaId}`)
}
