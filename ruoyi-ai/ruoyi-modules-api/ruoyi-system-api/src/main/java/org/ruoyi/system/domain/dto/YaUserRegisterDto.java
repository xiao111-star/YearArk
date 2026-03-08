package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册 DTO
 * <p>
 * passwordHash 为前端使用 SHA-256 对原始密码哈希后的十六进制字符串（64位）
 *
 * @author YearArk
 */
@Data
public class YaUserRegisterDto {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度须在 3~20 位之间")
    private String username;

    /**
     * 前端 SHA-256(原始密码) 后的十六进制字符串，固定 64 位
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 64, max = 64, message = "密码格式错误")
    private String passwordHash;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
}
