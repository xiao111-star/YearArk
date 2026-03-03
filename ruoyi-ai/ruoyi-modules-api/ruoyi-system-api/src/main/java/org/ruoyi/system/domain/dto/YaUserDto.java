package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户端用户 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaUserDto {

    private Integer id;

    @NotBlank(message = "用户名不能为空")
    private String username;
    private String passwordHash;
    private String email;
    private String avatarUrl;
    private Integer status;
}
