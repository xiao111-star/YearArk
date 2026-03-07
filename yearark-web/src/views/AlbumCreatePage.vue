<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import TemplateCard from '@/components/TemplateCard.vue'
import type { Template } from '@/components/TemplateCard.vue'
import { createAlbum } from '@/api/album'
import { listTemplates } from '@/api/template'

const router = useRouter()

const name = ref('')
const des = ref('')
const selectedTemplateId = ref<number | null>(null)
const templates = ref<Template[]>([])
const loading = ref(false)
const nameError = ref('')

async function fetchTemplates() {
  try {
    const res = await listTemplates()
    templates.value = res.data?.data ?? []
  } catch {
    templates.value = []
  }
}

function selectTemplate(id: number) {
  selectedTemplateId.value = id
}

async function handleSubmit() {
  nameError.value = ''

  if (!name.value.trim()) {
    nameError.value = '纪念册名称不能为空'
    return
  }

  loading.value = true
  try {
    const res = await createAlbum({
      name: name.value.trim(),
      des: des.value.trim() || undefined,
      templateId: selectedTemplateId.value ?? undefined,
    })
    const albumId = res.data?.data?.id ?? res.data?.data
    if (albumId) {
      router.push(`/album/${albumId}`)
    }
  } finally {
    loading.value = false
  }
}

onMounted(fetchTemplates)
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-8 py-6 px-4">
    <div>
      <h1 class="text-2xl font-bold">创建纪念册</h1>
      <p class="text-muted-foreground mt-1">填写基本信息并选择一个模板</p>
    </div>

    <!-- 基本信息 -->
    <div class="space-y-4">
      <div class="space-y-2">
        <Label for="album-name">名称 <span class="text-destructive">*</span></Label>
        <Input
          id="album-name"
          v-model="name"
          placeholder="请输入纪念册名称"
          :class="nameError ? 'border-destructive' : ''"
          @input="nameError = ''"
        />
        <p v-if="nameError" class="text-sm text-destructive">{{ nameError }}</p>
      </div>

      <div class="space-y-2">
        <Label for="album-des">描述</Label>
        <textarea
          id="album-des"
          v-model="des"
          placeholder="请输入纪念册描述（可选）"
          rows="3"
          class="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
        />
      </div>
    </div>

    <!-- 模板选择 -->
    <div class="space-y-4">
      <div>
        <h2 class="text-lg font-semibold">选择模板</h2>
        <p class="text-sm text-muted-foreground">点击选择一个模板样式</p>
      </div>

      <div
        v-if="templates.length"
        class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4"
      >
        <TemplateCard
          v-for="tpl in templates"
          :key="tpl.id"
          :template="tpl"
          :selected="selectedTemplateId === tpl.id"
          @select="selectTemplate"
        />
      </div>
      <p v-else class="text-sm text-muted-foreground">暂无可用模板</p>
    </div>

    <!-- 操作按钮 -->
    <div class="flex items-center gap-3 pt-4">
      <Button :disabled="loading" @click="handleSubmit">
        {{ loading ? '创建中...' : '创建' }}
      </Button>
      <Button variant="outline" @click="router.back()">取消</Button>
    </div>
  </div>
</template>
