package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 纪念册 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaAlbumDto {

    /** 纪念册 id（修改时必传） */
    private Integer id;

    @NotBlank(message = "纪念册名称不能为空")
    private String name;

    private String des;

    private Integer userId;


    private Integer templateId;

    /** 状态（0 草稿 1 发布） */
    private Integer status;

    /** 是否公开（0 否 1 是） */
    private Integer isPublic;

    private String pdfUrl;
}
