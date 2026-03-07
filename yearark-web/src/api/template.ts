import request from '@/utils/request'

/** 可用模板列表 */
export function listTemplates() {
  return request.get('/api/user/template/list')
}

/** 模板详情（含模板页列表） */
export function getTemplateDetail(id: number) {
  return request.get(`/api/user/template/${id}`)
}
