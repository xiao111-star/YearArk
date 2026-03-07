import request from '@/utils/request'

export interface DictItem {
  dictCode: number
  dictLabel: string
  dictValue: string
  dictType: string
  listClass: string
  cssClass: string
}

/** 根据字典类型查询字典数据 */
export function getDictByType(dictType: string) {
  return request.get<{ data: DictItem[] }>(`/api/user/dict/type/${dictType}`)
}
