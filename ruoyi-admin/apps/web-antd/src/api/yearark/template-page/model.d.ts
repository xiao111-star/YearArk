export interface YaTemplatePage {
  id: number;
  templateId: number;
  templateSchemaId?: number;
  content: string;
  previewUrl?: string;
  type: string;
  status: number;
  createAt?: string;
  updateAt?: string;
  createBy?: number;
  updateBy?: number;
}

export interface YaTemplatePageQuery {
  templateId?: number;
  type?: string;
  status?: number;
}
