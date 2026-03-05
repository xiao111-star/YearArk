package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 页面模板的 JSON Schema 对象 ya_template_schema
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_template_schema")
public class YaTemplateSchema implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * schema id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 内容（JSON Schema）
     */
    private String content;
    /**
     * 图片数量
     */
    private Integer imageCount;

    /**
     * 文字数量
     */
    private Integer textCount;

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
     * 创建者
     */
    private String createBy;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 状态（0 开启 1 停用）
     */
    private Integer status;

    /**
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
