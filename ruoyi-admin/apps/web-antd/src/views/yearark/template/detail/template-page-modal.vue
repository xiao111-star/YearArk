<script setup lang="ts">
import { computed, ref } from 'vue';

import { useVbenModal } from '@vben/common-ui';
import { DictEnum } from '@vben/constants';
import { cloneDeep, getPopupContainer } from '@vben/utils';

import { useVbenForm } from '#/adapter/form';
import {
  templatePageAdd,
  templatePageInfo,
  templatePageUpdate,
} from '#/api/yearark/template-page';
import { schemaList } from '#/api/yearark/template-schema';
import { getDictOptions } from '#/utils/dict';

const emit = defineEmits<{ reload: [] }>();

const isUpdate = ref(false);
const title = computed(() => {
  return isUpdate.value ? '编辑模板页' : '新增模板页';
});

const schemaOptions = ref<Array<{ label: string; value: number }>>([]);
async function loadSchemaOptions() {
  const list = await schemaList({});
  schemaOptions.value = list.map((item) => ({
    label: item.name,
    value: item.id,
  }));
}

const [BasicForm, formApi] = useVbenForm({
  commonConfig: {
    labelWidth: 100,
  },
  schema: [
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'id',
      label: 'ID',
    },
    {
      component: 'Input',
      dependencies: { show: () => false, triggerFields: [''] },
      fieldName: 'templateId',
      label: '模板ID',
    },
    {
      component: 'Select',
      componentProps: {
        getPopupContainer,
        options: getDictOptions('ya_template_page_type', true),
        style: { width: '100%' },
      },
      fieldName: 'type',
      label: '页面类型',
      rules: 'required',
    },
    {
      component: 'Select',
      componentProps: () => ({
        getPopupContainer,
        options: schemaOptions.value,
        allowClear: true,
        showSearch: true,
        style: { width: '100%' },
        filterOption: (input: string, option: any) =>
          option.label.toLowerCase().includes(input.toLowerCase()),
      }),
      fieldName: 'templateSchemaId',
      label: '关联Schema',
      rules: 'required',
    },
    {
      component: 'Textarea',
      componentProps: {
        autoSize: { minRows: 8, maxRows: 20 },
        placeholder: '请输入H5模板内容',
      },
      fieldName: 'content',
      formItemClass: 'items-start',
      label: 'H5模板内容',
      rules: 'required',
    },
    {
      component: 'ImageUpload',
      componentProps: {
        maxNumber: 1,
        maxSize: 50,
        resultField: 'url',
      },
      fieldName: 'previewUrl',
      formItemClass: 'items-start',
      label: '预览图',
    },
    {
      component: 'RadioGroup',
      componentProps: {
        buttonStyle: 'solid',
        options: getDictOptions(DictEnum.SYS_NORMAL_DISABLE, true),
        optionType: 'button',
      },
      defaultValue: 0,
      fieldName: 'status',
      label: '状态',
    },
  ],
  showDefaultActions: false,
});

const [BasicModal, modalApi] = useVbenModal({
  fullscreenButton: false,
  onCancel: handleCancel,
  onConfirm: handleConfirm,
  onOpenChange: async (isOpen) => {
    if (!isOpen) {
      return null;
    }
    modalApi.modalLoading(true);

    await loadSchemaOptions();

    const { id, templateId } = modalApi.getData() as {
      id?: number;
      templateId: number;
    };
    isUpdate.value = !!id;

    await formApi.setValues({ templateId });

    if (isUpdate.value && id) {
      const record = await templatePageInfo(id);
      await formApi.setValues(record);
    }

    modalApi.modalLoading(false);
  },
});

async function handleConfirm() {
  try {
    modalApi.modalLoading(true);
    const { valid } = await formApi.validate();
    if (!valid) {
      return;
    }
    const data = cloneDeep(await formApi.getValues());
    await (isUpdate.value ? templatePageUpdate(data) : templatePageAdd(data));
    emit('reload');
    await handleCancel();
  } catch (error) {
    console.error(error);
  } finally {
    modalApi.modalLoading(false);
  }
}

async function handleCancel() {
  modalApi.close();
  await formApi.resetForm();
}
</script>

<template>
  <BasicModal :close-on-click-modal="false" :title="title" class="w-[700px]">
    <BasicForm />
  </BasicModal>
</template>
