package org.ruoyi.system.domain.dto;

import lombok.Data;

/**
 * 纪念册查询 DTO
 *
 * @author YearArk
 */
@Data
public class YaAlbumQueryDto {

    private String name;
    private Integer userId;
    private Integer templateId;
    private Integer status;
    private Integer isPublic;
}
