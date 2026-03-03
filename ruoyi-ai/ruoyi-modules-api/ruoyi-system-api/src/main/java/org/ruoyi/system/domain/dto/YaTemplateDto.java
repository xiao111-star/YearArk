package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模板套件 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaTemplateDto {

    private Integer id;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private Integer type;
    private String previewUrl;
    private String des;
    private Integer status;
}
