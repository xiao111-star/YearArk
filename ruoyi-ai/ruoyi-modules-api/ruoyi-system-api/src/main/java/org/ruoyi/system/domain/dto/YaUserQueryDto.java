package org.ruoyi.system.domain.dto;

import lombok.Data;

/**
 * 用户端用户查询 DTO
 *
 * @author YearArk
 */
@Data
public class YaUserQueryDto {

    private String username;
    private String email;
    private Integer status;
}
