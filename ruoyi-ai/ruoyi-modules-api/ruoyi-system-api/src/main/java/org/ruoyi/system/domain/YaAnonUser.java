package org.ruoyi.system.domain;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 匿名上传者信息（存储在 StpAnonUtil session 中）
 */
@Data
public class YaAnonUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 邀请token ID
     */
    private Integer tokenId;

    /**
     * 纪念册ID
     */
    private Integer albumId;

    /**
     * 邀请ID
     */
    private Integer inviteId;
}
