package org.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 匿名 token VO（验证访问码后返回）
 *
 * @author YearArk
 */
@Data
public class AnonTokenVo {

    /** StpAnonUtil token */
    private String token;

    private Integer albumId;

    private String albumName;
}
