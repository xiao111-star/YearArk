package org.ruoyi.system.domain.vo.client;

import lombok.Data;

/**
 * 用户端登录返回 VO
 *
 * @author YearArk
 */
@Data
public class YaLoginVo {

    /** StpUserUtil token */
    private String token;

    private Integer userId;

    private String username;
}
