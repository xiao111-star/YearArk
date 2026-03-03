package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 纪念册单页 VO
 *
 * @author YearArk
 */
@Data
public class YaAlbumPageVo {

    private Integer id;
    private Integer albumId;
    private Integer templatePageId;
    private String des;
    private Integer sort;
    private String data;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
