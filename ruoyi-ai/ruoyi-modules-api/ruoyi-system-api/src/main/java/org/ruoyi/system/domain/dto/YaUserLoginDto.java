package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录 DTO
 * <p>
 * passwordHash 为前端使用 SHA-256 对原始密码哈希后的十六进制字符串
 *
 * @author YearArk
 */
@Data
public class YaUserLoginDto {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 前端 SHA-256(原始密码) 后的十六进制字符串
     */
    @NotBlank(message = "密码不能为空")
    private String passwordHash;
}
