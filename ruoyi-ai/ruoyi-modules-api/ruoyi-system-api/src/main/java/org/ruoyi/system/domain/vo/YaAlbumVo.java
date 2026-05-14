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

    /** 生成状态（字典：ya_album_generation_status） */
    private Integer generationStatus;

    /** 生成失败原因 */
    private String generationFailReason;

    /** 关联展示字段 */
    private String userName;
    private String templateName;

    /** 封面图 URL：取素材库中首张审核通过的图片（type=2, status=2，按 sort 升序） */
    private String coverUrl;
}
