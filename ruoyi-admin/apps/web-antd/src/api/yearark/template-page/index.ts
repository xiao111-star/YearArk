import type { YaTemplatePage, YaTemplatePageQuery } from './model';

import type { ID, IDS, PageQuery, PageResult } from '#/api/common';

import { requestClient } from '#/api/request';

enum Api {
  root = '/yearark/template-page',
  templatePageList = '/yearark/template-page/list',
  templatePagePage = '/yearark/template-page/page',
}

export function templatePagePage(params?: PageQuery) {
  return requestClient.get<PageResult<YaTemplatePage>>(Api.templatePagePage, {
    params,
  });
}

export function templatePageList(params?: YaTemplatePageQuery) {
  return requestClient.get<YaTemplatePage[]>(Api.templatePageList, { params });
}

export function templatePageInfo(id: ID) {
  return requestClient.get<YaTemplatePage>(`${Api.root}/${id}`);
}

export function templatePageAdd(data: Partial<YaTemplatePage>) {
  return requestClient.postWithMsg<void>(Api.root, data);
}

export function templatePageUpdate(data: Partial<YaTemplatePage>) {
  return requestClient.postWithMsg<void>(`${Api.root}/update`, data);
}

export function templatePageRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
