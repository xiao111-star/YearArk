package org.ruoyi.system.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
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
    
    @NotBlank(message = "访问码不能为空")
    private String accessCode;
    
    private Integer status;
    
    @NotNull(message = "过期时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime expireAt;
}
