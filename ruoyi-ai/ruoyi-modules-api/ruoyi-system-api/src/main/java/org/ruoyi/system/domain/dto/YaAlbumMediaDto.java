package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 纪念册素材 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaAlbumMediaDto {

    private Integer id;

    @NotNull(message = "纪念册ID不能为空")
    private Integer albumId;

    private Integer tokenId;

    @NotNull(message = "类型不能为空")
    private Integer type;

    private String content;
    private Integer sort;
    private Double size;
    private Integer facesCount;
    private String tags;
    private Integer status;
}
