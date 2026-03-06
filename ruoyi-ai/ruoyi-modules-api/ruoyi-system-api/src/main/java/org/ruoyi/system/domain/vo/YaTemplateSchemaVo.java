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
    private String name;
    private String content;
    private Integer imageCount;
    private Integer textCount;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private String createBy;
    private String updateBy;

    /** 被引用的模板页面数量 */
    private Long usageCount;
}
