package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邀请链接对象 ya_invite
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_invite")
public class YaInvite implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分享 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 纪念册 id
     */
    private Integer albumId;

    /**
     * 随机生成邀请码 6 位随机串
     */
    private String inviteCode;

    /**
     * 访问码
     */
    private String accessCode;

    /**
     * 状态（0 开启 1 停用）
     */
    private Integer status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createAt;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
