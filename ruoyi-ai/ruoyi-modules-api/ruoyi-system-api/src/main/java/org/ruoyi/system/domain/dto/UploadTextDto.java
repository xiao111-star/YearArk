package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 匿名上传文字 DTO
 *
 * @author YearArk
 */
@Data
public class UploadTextDto {

    @NotBlank(message = "文字内容不能为空")
    private String content;
}
