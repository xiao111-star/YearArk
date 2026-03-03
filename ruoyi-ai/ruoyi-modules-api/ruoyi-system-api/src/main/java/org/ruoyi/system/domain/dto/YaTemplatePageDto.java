package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 套件内的页面模板 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaTemplatePageDto {

    private Integer id;

    @NotNull(message = "模板ID不能为空")
    private Integer templateId;

    @NotNull(message = "Schema ID不能为空")
    private Integer templateSchemaId;

    private String content;
    private String previewUrl;
    private String type;
    private Integer Status;
}
