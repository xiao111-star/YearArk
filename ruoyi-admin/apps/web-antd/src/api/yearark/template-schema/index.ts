import type { YaTemplateSchema, YaTemplateSchemaQuery } from './model';

import type { ID, IDS, PageQuery, PageResult } from '#/api/common';

import { requestClient } from '#/api/request';

enum Api {
  root = '/yearark/template-schema',
  schemaList = '/yearark/template-schema/list',
  schemaPage = '/yearark/template-schema/page',
}

export function schemaPage(params?: PageQuery) {
  return requestClient.get<PageResult<YaTemplateSchema>>(Api.schemaPage, {
    params,
  });
}

export function schemaList(params?: YaTemplateSchemaQuery) {
  return requestClient.get<YaTemplateSchema[]>(Api.schemaList, { params });
}

export function schemaInfo(id: ID) {
  return requestClient.get<YaTemplateSchema>(`${Api.root}/${id}`);
}

export function schemaAdd(data: Partial<YaTemplateSchema>) {
  return requestClient.postWithMsg<void>(Api.root, data);
}

export function schemaUpdate(data: Partial<YaTemplateSchema>) {
  return requestClient.postWithMsg<void>(`${Api.root}/update`, data);
}

export function schemaRemove(ids: IDS) {
  return requestClient.deleteWithMsg<void>(`${Api.root}/${ids}`);
}
