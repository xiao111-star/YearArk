package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模板套件 VO
 *
 * @author YearArk
 */
@Data
public class YaTemplateVo {

    private Integer id;
    private String name;
    private Integer type;
    private String previewUrl;
    private String des;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private Integer createBy;
    private Integer updateBy;
    private String createByName;
    private String updateByName;
}
