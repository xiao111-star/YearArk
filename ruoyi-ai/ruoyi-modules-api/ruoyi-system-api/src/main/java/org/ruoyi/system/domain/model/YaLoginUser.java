package org.ruoyi.system.domain.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户端已登录用户信息（存储在 StpUserUtil session 中）
 */
@Data
public class YaLoginUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 用户名
     */
    private String username;
}
