package org.ruoyi.system.domain.item;

import lombok.Data;

@Data
public class TemplatePageItem {
    /** 模板页 ID */
    private Integer templatePageId;
    /** 关联 Schema ID */
    private Integer schemaId;
    /** 需要的图片 slot 数量 */
    private Integer imageCount;
    /** 需要的文字 slot 数量 */
    private Integer textCount;
    /** 页面类型中文名（如：封面、章节页、内容页） */
    private String pageTypeName;
    /** Schema JSON 内容，包含各 slot 的 label、maxLength 等约束，供 AI 生成文字时参考 */
    private String schemaContent;
}
