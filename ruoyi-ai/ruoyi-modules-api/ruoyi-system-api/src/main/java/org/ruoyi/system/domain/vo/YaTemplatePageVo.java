package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 套件内的页面模板 VO
 *
 * @author YearArk
 */
@Data
public class YaTemplatePageVo {

    private Integer id;
    private Integer templateId;
    private Integer templateSchemaId;
    private String content;
    private String previewUrl;
    private Integer type;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Integer createBy;
    private Integer updateBy;
}
