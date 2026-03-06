export interface YaTemplateSchema {
  id: number;
  name?: string;
  content: string;
  imageCount?: number;
  textCount?: number;
  status: number;
  createAt?: string;
  updateAt?: string;
  createBy?: string;
  updateBy?: string;
  usageCount?: number;
}

export interface YaTemplateSchemaQuery {
  status?: number;
}
