import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { h } from 'vue';

import { getPopupContainer } from '@vben/utils';

import { Image, Tag } from 'ant-design-vue';

import { getDictOptions } from '#/utils/dict';
import { renderDict } from '#/utils/render';

export const querySchema: FormSchemaGetter = () => [
  {
    component: 'Input',
    fieldName: 'name',
    label: '模板名称',
  },
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: getDictOptions('ya_template_type'),
    },
    fieldName: 'type',
    label: '模板类型',
  },
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
    title: '模板ID',
    field: 'id',
    width: 80,
  },
  {
    title: '模板名称',
    field: 'name',
  },
  {
    title: '模板类型',
    field: 'type',
    slots: {
      default: ({ row }) => {
        return renderDict(row.type, 'ya_template_type');
      },
    },
  },
  {
    title: '预览图',
    field: 'previewUrl',
    width: 100,
    slots: {
      default: ({ row }) => {
        if (!row.previewUrl) {
          return h('span', '-');
        }
        return h(Image, {
          src: row.previewUrl,
          width: 60,
          height: 60,
          style: { objectFit: 'cover' },
          fallback:
            'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mN8/+F/PQAJpAN42kLcEQAAAABJRU5ErkJggg==',
        });
      },
    },
  },
  {
    title: '描述',
    field: 'des',
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
    title: '使用数量',
    field: 'albumCount',
    width: 100,
  },
  {
    title: '创建时间',
    field: 'createAt',
    width: 180,
  },
  {
    title: '更新时间',
    field: 'updateAt',
    width: 180,
  },
  {
    title: '创建人',
    field: 'createByName',
    width: 100,
  },
  {
    title: '更新者',
    field: 'updateByName',
    width: 100,
  },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 200,
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
    label: '模板ID',
  },
  {
    component: 'Input',
    fieldName: 'name',
    label: '模板名称',
    rules: 'required',
  },
  {
    component: 'Select',
    componentProps: {
      getPopupContainer,
      options: getDictOptions('ya_template_type'),
    },
    fieldName: 'type',
    label: '模板类型',
    rules: 'required',
  },
  {
    component: 'Input',
    fieldName: 'previewUrl',
    label: '预览图URL',
  },
  {
    component: 'Textarea',
    componentProps: {
      autoSize: true,
    },
    fieldName: 'des',
    formItemClass: 'items-start',
    label: '描述',
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
