package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 匿名上传者虚拟身份 VO
 *
 * @author YearArk
 */
@Data
public class YaInviteTokenVo {

    private Integer id;
    private Integer albumId;
    private Integer inviteId;
    private String token;
    private String ipAddress;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
    private LocalDateTime expiredAt;
    private Integer status;
}
