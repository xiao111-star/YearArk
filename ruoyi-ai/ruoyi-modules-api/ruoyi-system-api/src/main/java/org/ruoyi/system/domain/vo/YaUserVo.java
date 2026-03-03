package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户端用户 VO
 *
 * @author YearArk
 */
@Data
public class YaUserVo {

    private Integer id;
    private String username;
    private String email;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
