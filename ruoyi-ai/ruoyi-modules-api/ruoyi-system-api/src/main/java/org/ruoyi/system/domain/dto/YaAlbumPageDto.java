package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 纪念册页面 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaAlbumPageDto {

    private Integer id;

    @NotNull(message = "纪念册ID不能为空")
    private Integer albumId;

    @NotNull(message = "模板页ID不能为空")
    private Integer templatePageId;

    private String des;

    @NotNull(message = "排序不能为空")
    private Integer sort;

    private String data;
}
