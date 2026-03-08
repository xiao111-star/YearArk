package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 纪念册对象 ya_album
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_album")
public class YaAlbum implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 纪念册 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 纪念册名称
     */
    private String name;

    /**
     * 纪念册描述
     */
    private String des;

    /**
     * 创建者 ID
     */
    private Integer userId;

    /**
     * 主题 id
     */
    private Integer templateId;

    /**
     * 状态（0 草稿 1 发布）
     */
    private Integer status;

    /**
     * 是否公开放在首页（0 否 1 是）
     */
    private Integer isPublic;

    /**
     * 生成 PDF 的 ossURL
     */
    private String pdfUrl;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;

    /**
     * 生成状态：0 待生成 / 1 生成中 / 2 生成完成 / 3 生成失败
     * 字典类型：ya_album_generation_status
     */
    @TableField("generation_status")
    private Integer generationStatus;

    /**
     * 生成失败原因
     */
    @TableField("generation_fail_reason")
    private String generationFailReason;

    /**
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
