import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { h } from 'vue';

import { getPopupContainer } from '@vben/utils';

import { Tag } from 'ant-design-vue';

export const querySchema: FormSchemaGetter = () => [
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
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
    width: 100,
  },
  {
    title: '图片数量',
    field: 'imageCount',
    width: 100,
  },
  {
    title: '文字数量',
    field: 'textCount',
    width: 100,
  },
  {
    title: '使用数量',
    field: 'usageCount',
    width: 100,
  },
  {
    title: '状态',
    field: 'status',
    width: 80,
    slots: {
      default: ({ row }) => {
        return row.status === 1
          ? h(Tag, { color: 'green' }, () => '启用')
          : h(Tag, { color: 'red' }, () => '禁用');
      },
    },
  },
  {
    title: '创建时间',
    field: 'createAt',
    width: 180,
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
      options: [
        { label: '启用', value: 1 },
        { label: '禁用', value: 0 },
      ],
      optionType: 'button',
    },
    defaultValue: 1,
    fieldName: 'status',
    label: '状态',
  },
];
