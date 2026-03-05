<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

import type { VxeGridProps } from '#/adapter/vxe-table';
import type { YaTemplatePage } from '#/api/yearark/template-page/model';
import type { YaTemplate } from '#/api/yearark/template/model';

import { Page, useVbenModal } from '@vben/common-ui';
import { getVxePopupContainer } from '@vben/utils';

import {
  Card,
  Descriptions,
  DescriptionsItem,
  Image,
  Modal,
  Popconfirm,
  Space,
  Statistic,
  Tag,
} from 'ant-design-vue';

import { useVbenVxeGrid, vxeCheckboxChecked } from '#/adapter/vxe-table';
import { templateInfo } from '#/api/yearark/template';
import {
  templatePagePage,
  templatePageRemove,
} from '#/api/yearark/template-page';

import templatePageModal from './template-page-modal.vue';

const route = useRoute();
const router = useRouter();
const templateId = computed(() => Number(route.params.id));
const templateData = ref<null | YaTemplate>(null);

async function loadTemplateInfo() {
  templateData.value = await templateInfo(templateId.value);
}

onMounted(() => {
  loadTemplateInfo();
});

const pageColumns: VxeGridProps['columns'] = [
  { type: 'checkbox', width: 60 },
  { title: '页面ID', field: 'id', width: 80 },
  { title: '关联Schema', field: 'templateSchemaId', width: 120 },
  { title: '页面类型', field: 'type', width: 120 },
  {
    title: '预览图',
    field: 'previewUrl',
    width: 100,
    slots: {
      default: ({ row }: { row: YaTemplatePage }) => {
        if (!row.previewUrl) {
          return h('span', '-');
        }
        return h(Image, {
          src: row.previewUrl,
          width: 60,
          height: 60,
          style: { objectFit: 'cover' },
        });
      },
    },
  },
  {
    title: '状态',
    field: 'status',
    width: 80,
    slots: {
      default: ({ row }: { row: YaTemplatePage }) => {
        return row.status === 1
          ? h(Tag, { color: 'green' }, () => '启用')
          : h(Tag, { color: 'red' }, () => '禁用');
      },
    },
  },
  { title: '创建时间', field: 'createAt', width: 180 },
  {
    field: 'action',
    fixed: 'right',
    slots: { default: 'action' },
    title: '操作',
    width: 240,
  },
];

const gridOptions: VxeGridProps = {
  checkboxConfig: {
    highlight: true,
    reserve: true,
  },
  columns: pageColumns,
  height: 500,
  keepSource: true,
  pagerConfig: {},
  proxyConfig: {
    ajax: {
      query: async ({ page }) => {
        return await templatePagePage({
          pageNum: page.currentPage,
          pageSize: page.pageSize,
          templateId: templateId.value,
        });
      },
    },
  },
  rowConfig: {
    keyField: 'id',
  },
  id: 'yearark-template-page-index',
};

const [BasicTable, tableApi] = useVbenVxeGrid({ gridOptions });
const [TemplatePageModal, modalApi] = useVbenModal({
  connectedComponent: templatePageModal,
});

function handleAddPage() {
  modalApi.setData({ templateId: templateId.value });
  modalApi.open();
}

function handleEditPage(row: YaTemplatePage) {
  modalApi.setData({ id: row.id, templateId: templateId.value });
  modalApi.open();
}

async function handleDeletePage(row: YaTemplatePage) {
  await templatePageRemove([row.id]);
  await tableApi.query();
}

function handleMultiDeletePage() {
  const rows = tableApi.grid.getCheckboxRecords();
  const ids = rows.map((row: YaTemplatePage) => row.id);
  Modal.confirm({
    title: '提示',
    okType: 'danger',
    content: `确认删除选中的${ids.length}条记录吗？`,
    onOk: async () => {
      await templatePageRemove(ids);
      await tableApi.query();
    },
  });
}

const previewVisible = ref(false);
const previewContent = ref('');

function handlePreview(row: YaTemplatePage) {
  previewContent.value = row.content || '';
  previewVisible.value = true;
}

function handleBack() {
  router.push('/yearark/template');
}
</script>

<template>
  <Page>
    <div class="mb-4">
      <a-button @click="handleBack">返回列表</a-button>
    </div>

    <Card v-if="templateData" title="模板基本信息" class="mb-4">
      <Descriptions :column="3" bordered>
        <DescriptionsItem label="模板名称">
          {{ templateData.name }}
        </DescriptionsItem>
        <DescriptionsItem label="模板类型">
          {{ templateData.typeName || templateData.type }}
        </DescriptionsItem>
        <DescriptionsItem label="状态">
          <Tag :color="templateData.status === 1 ? 'green' : 'red'">
            {{ templateData.status === 1 ? '启用' : '禁用' }}
          </Tag>
        </DescriptionsItem>
        <DescriptionsItem label="预览图">
          <Image
            v-if="templateData.previewUrl"
            :src="templateData.previewUrl"
            :width="120"
          />
          <span v-else>-</span>
        </DescriptionsItem>
        <DescriptionsItem label="描述" :span="2">
          {{ templateData.des || '-' }}
        </DescriptionsItem>
        <DescriptionsItem label="创建时间">
          {{ templateData.createAt }}
        </DescriptionsItem>
        <DescriptionsItem label="更新时间">
          {{ templateData.updateAt }}
        </DescriptionsItem>
        <DescriptionsItem label="使用数量">
          <Statistic :value="templateData.albumCount || 0" suffix="个纪念册" />
        </DescriptionsItem>
      </Descriptions>
    </Card>

    <Card title="模板页面列表">
      <BasicTable>
        <template #toolbar-tools>
          <Space>
            <a-button
              :disabled="!vxeCheckboxChecked(tableApi)"
              danger
              type="primary"
              v-access:code="['yearark:templatePage:remove']"
              @click="handleMultiDeletePage"
            >
              批量删除
            </a-button>
            <a-button
              type="primary"
              v-access:code="['yearark:templatePage:add']"
              @click="handleAddPage"
            >
              新增模板页
            </a-button>
          </Space>
        </template>
        <template #action="{ row }">
          <Space>
            <ghost-button @click.stop="handlePreview(row)">预览</ghost-button>
            <ghost-button
              v-access:code="['yearark:templatePage:edit']"
              @click.stop="handleEditPage(row)"
            >
              编辑
            </ghost-button>
            <Popconfirm
              :get-popup-container="getVxePopupContainer"
              placement="left"
              title="确认删除？"
              @confirm="handleDeletePage(row)"
            >
              <ghost-button
                danger
                v-access:code="['yearark:templatePage:remove']"
                @click.stop=""
              >
                删除
              </ghost-button>
            </Popconfirm>
          </Space>
        </template>
      </BasicTable>
    </Card>

    <TemplatePageModal
      @reload="
        tableApi.query();
        loadTemplateInfo();
      "
    />

    <Modal
      v-model:open="previewVisible"
      title="模板页预览"
      :footer="null"
      width="800px"
      destroy-on-close
    >
      <div style="width: 100%; height: 600px">
        <iframe
          :srcdoc="previewContent"
          style="width: 100%; height: 100%; border: none"
          sandbox="allow-scripts"
        ></iframe>
      </div>
    </Modal>
  </Page>
</template>
