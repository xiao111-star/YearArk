package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 邀请链接 DTO（新增/修改）
 *
 * @author YearArk
 */
@Data
public class YaInviteDto {

    private Integer id;

    @NotNull(message = "纪念册ID不能为空")
    private Integer albumId;

    private String inviteCode;
    private String accessCode;
    private Integer status;
    private LocalDateTime expireAt;
}
