package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 纪念册素材 VO
 *
 */
@Data
public class YaAlbumMediaVo {
    private Integer id;
    private Integer albumId;
    private Integer tokenId;
    private Integer type;
    private String content;
    private Integer sort;
    private Double size;
    private Integer facesCount;
    private String tags;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
