package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 页面模板 JSON Schema DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaTemplateSchemaDto {

    private Integer id;
    private String name;
    private String content;
    private Integer imageCount;
    private Integer textCount;
    private Integer status;
}
