package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请链接 VO
 *
 * @author YearArk
 */
@Data
public class YaInviteVo {

    private Integer id;
    private Integer albumId;
    private String inviteCode;
    private String accessCode;
    private Integer status;
    private LocalDateTime createAt;
    private LocalDateTime expireAt;
}
