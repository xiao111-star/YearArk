package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 页面模板 JSON Schema VO
 *
 * @author YearArk
 */
@Data
public class YaTemplateSchemaVo {

    private Integer id;
    private String content;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String createBy;
    private String updateBy;
}
