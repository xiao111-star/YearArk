package org.ruoyi.system.domain.vo;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class YaAlbumVo {
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
     * 创建者名称
     */
    private String userName;

    /**
     * 模板 id
     */
    private Integer templateId;

    /**
     * 模板名称
     */
    private String templateName;

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

}
