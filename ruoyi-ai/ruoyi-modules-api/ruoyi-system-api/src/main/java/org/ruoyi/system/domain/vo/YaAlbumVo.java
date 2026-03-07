package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 纪念册 VO
 *
 * @author YearArk
 */
@Data
public class YaAlbumVo {

    private Integer id;
    private String name;
    private String des;
    private Integer userId;
    private Integer templateId;
    private Integer status;
    private Integer isPublic;
    private String pdfUrl;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    /** 关联展示字段 */
    private String userName;
    private String templateName;
}
