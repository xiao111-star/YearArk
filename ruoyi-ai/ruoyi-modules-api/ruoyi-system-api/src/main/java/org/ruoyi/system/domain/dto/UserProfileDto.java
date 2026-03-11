package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户个人资料更新 DTO
 */
@Data
public class UserProfileDto {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String email;
}
