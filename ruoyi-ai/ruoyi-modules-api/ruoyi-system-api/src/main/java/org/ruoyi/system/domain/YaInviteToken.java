package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 匿名上传者虚拟身份对象 ya_invite_token
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_invite_token")
public class YaInviteToken implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 虚拟用户 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 纪念册 id
     */
    private Integer albumId;

    /**
     * 邀请 id
     */
    private Integer inviteId;

    /**
     * JWT token
     */
    private String token;

    /**
     * ip 地址
     */
    private String ipAddress;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createAt;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;

    /**
     * 过期时间（放 redis 中）
     */
    private LocalDateTime expiredAt;

    /**
     * 状态（0 开启 1 停用）
     */
    private Integer status;

}
