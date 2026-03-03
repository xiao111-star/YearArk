package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 匿名上传者虚拟身份 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaInviteTokenDto {

    private Integer id;

    @NotNull(message = "纪念册ID不能为空")
    private Integer albumId;

    @NotNull(message = "邀请ID不能为空")
    private Integer inviteId;

    private String token;
    private String ipAddress;
    private LocalDateTime expiredAt;
    private Integer status;
}
