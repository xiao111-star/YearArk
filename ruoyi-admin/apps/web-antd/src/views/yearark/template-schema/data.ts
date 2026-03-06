import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { DictEnum } from '@vben/constants';
import { getPopupContainer } from '@vben/utils';

import { getDictOptions } from '#/utils/dict';
import { renderDict } from '#/utils/render';

export const querySchema: FormSchemaGetter = () => [
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
    },
    fieldName: 'status',
    label: '状态',
  },
];

export const columns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60 },
  {
    title: 'Schema ID',
    field: 'id',
    minWidth: 100,
  },
  {
    title: 'Schema名称',
    field: 'name',
    minWidth: 200,
  },
  {
    title: '图片数量',
    field: 'imageCount',
    minWidth: 100,
  },
  {
    title: '文字数量',
    field: 'textCount',
    minWidth: 100,
  },
  {
    title: '使用数量',
    field: 'usageCount',
    minWidth: 100,
  },
  {
    title: '状态',
    field: 'status',
    minWidth: 100,
    slots: {
      default: ({ row }) => {
        return renderDict(row.status, DictEnum.SYS_NORMAL_DISABLE);
      },
    },
  },
  {
    title: '创建时间',
    field: 'createAt',
    minWidth: 180,
  },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 180,
  },
];

export const modalSchema: FormSchemaGetter = () => [
  {
    component: 'Input',
    dependencies: {
      show: () => false,
      triggerFields: [''],
    },
    fieldName: 'id',
    label: 'Schema ID',
  },
  {
    component: 'Input',
    componentProps: {
      placeholder: '请输入Schema名称',
    },
    fieldName: 'name',
    label: 'Schema名称',
    rules: 'required',
  },
  {
    component: 'Textarea',
    componentProps: {
      autoSize: { minRows: 10, maxRows: 25 },
      placeholder: '请输入JSON Schema内容',
    },
    fieldName: 'content',
    formItemClass: 'items-start',
    label: 'JSON内容',
    rules: 'required',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
      placeholder: '请输入图片数量',
    },
    defaultValue: 0,
    fieldName: 'imageCount',
    label: '图片数量',
    rules: 'required',
  },
  {
    component: 'InputNumber',
    componentProps: {
      min: 0,
      placeholder: '请输入文字数量',
    },
    defaultValue: 0,
    fieldName: 'textCount',
    label: '文字数量',
    rules: 'required',
  },
  {
    component: 'RadioGroup',
    componentProps: {
      buttonStyle: 'solid',
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
      optionType: 'button',
    },
    defaultValue: 0,
    fieldName: 'status',
    label: '状态',
  },
];
