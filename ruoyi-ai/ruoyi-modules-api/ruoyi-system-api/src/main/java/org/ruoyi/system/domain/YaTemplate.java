package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板套件对象 ya_template
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_template")
public class YaTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 模板名称
     */
    private String name;

    /**
     * 模板类型（存字典）
     */
    private Integer type;

    /**
     * 预览图片 url
     */
    private String previewUrl;

    /**
     * 描述（给ai看的）
     */
    private String des;

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
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;

    /**
     * 创建者
     */
    private Integer createBy;

    /**
     * 更新者
     */
    private Integer updateBy;

    /**
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
