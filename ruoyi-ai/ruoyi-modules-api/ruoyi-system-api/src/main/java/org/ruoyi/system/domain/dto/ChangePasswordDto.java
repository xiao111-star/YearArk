package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 修改密码 DTO（passwordHash 均为前端 SHA-256 后的值）
 */
@Data
public class ChangePasswordDto {

    @NotBlank(message = "旧密码不能为空")
    private String oldPasswordHash;

    @NotBlank(message = "新密码不能为空")
    private String newPasswordHash;
}
