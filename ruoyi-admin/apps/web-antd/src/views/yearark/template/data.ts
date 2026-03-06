import type { FormSchemaGetter } from '#/adapter/form';
import type { VxeGridProps } from '#/adapter/vxe-table';

import { h } from 'vue';

import { DictEnum } from '@vben/constants';
import { getPopupContainer } from '@vben/utils';

import { Image } from 'ant-design-vue';

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
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
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
    minWidth: 80,
  },
  {
    title: '模板名称',
    field: 'name',
    minWidth: 120,
  },
  {
    title: '模板类型',
    field: 'type',
    minWidth: 100,
    slots: {
      default: ({ row }) => {
        return renderDict(row.type, 'ya_template_type');
      },
    },
  },
  {
    title: '预览图',
    field: 'previewUrl',
    minWidth: 100,
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
    minWidth: 120,
  },
  {
    title: '状态',
    field: 'status',
    minWidth: 80,
    slots: {
      default: ({ row }) => {
        return renderDict(row.status, DictEnum.SYS_NORMAL_DISABLE);
      },
    },
  },
  {
    title: '使用数量',
    field: 'albumCount',
    minWidth: 100,
  },
  {
    title: '创建时间',
    field: 'createAt',
    minWidth: 180,
  },
  {
    title: '更新时间',
    field: 'updateAt',
    minWidth: 180,
  },
  {
    title: '创建人',
    field: 'createByName',
    minWidth: 100,
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
      options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE),
      optionType: 'button',
    },
    defaultValue: 0,
    fieldName: 'status',
    label: '状态',
  },
];
