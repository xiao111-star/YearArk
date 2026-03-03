package org.ruoyi.system.domain.dto;

import lombok.Data;

/**
 * 纪念册素材查询 DTO
 *
 * @author YearArk
 */
@Data
public class YaAlbumMediaQueryDto {

    private Integer albumId;
    private Integer type;
    private Integer status;
    private String tags;
}
