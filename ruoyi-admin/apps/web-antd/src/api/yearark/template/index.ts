import type { YaTemplate, YaTemplateQuery } from './model';

import type { ID, IDS, PageQuery, PageResult } from '#/api/common';

import { requestClient } from '#/api/request';

enum Api {
  root = '/yearark/template',
  templateList = '/yearark/template/list',
  templatePage = '/yearark/template/page',
}

export function templatePage(params?: PageQuery) {
  return requestClient.get<PageResult<YaTemplate>>(Api.templatePage, {
    params,
  });
}

export function templateList(params?: YaTemplateQuery) {
  return requestClient.get<YaTemplate[]>(Api.templateList, { params });
}

export function templateInfo(id: ID) {
  return requestClient.get<YaTemplate>(`${Api.root}/${id}`);
}

export function templateAdd(data: Partial<YaTemplate>) {
  return requestClient.postWithMsg<void>(Api.root, data);
}

export function templateUpdate(data: Partial<YaTemplate>) {
  return requestClient.postWithMsg<void>(`${Api.root}/update`, data);
}

export function templateRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
