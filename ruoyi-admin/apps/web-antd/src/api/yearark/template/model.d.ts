export interface YaTemplate {
  id: number;
  name: string;
  type: number;
  typeName?: string;
  previewUrl?: string;
  des?: string;
  status: number;
  createAt?: string;
  updateAt?: string;
  createBy?: number;
  updateBy?: number;
  createByName?: string;
  updateByName?: string;
  albumCount?: number;
}

export interface YaTemplateQuery {
  name?: string;
  type?: number;
  status?: number;
}
