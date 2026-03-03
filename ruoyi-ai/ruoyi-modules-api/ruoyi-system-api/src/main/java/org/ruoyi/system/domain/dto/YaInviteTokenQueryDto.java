package org.ruoyi.system.domain.dto;

import lombok.Data;

/**
 * 匿名上传者虚拟身份查询 DTO
 *
 * @author YearArk
 */
@Data
public class YaInviteTokenQueryDto {

    private Integer albumId;
    private Integer inviteId;
    private Integer status;
}
