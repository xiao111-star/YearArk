package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 套件内的页面模板对象 ya_template_page
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_template_page")
public class YaTemplatePage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 模板页id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联模板id
     */
    private Integer templateId;

    /**
     * 关联的schema id
     */
    private Integer templateSchemaId;

    /**
     * 模板h5字符串
     */
    private String content;

    /**
     * 单页预览链接
     */
    private String previewUrl;

    /**
     * 类型
     */
    private String type;

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
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateAt;

    /**
     * 创建者
     */
    private Integer createBy;

    /**
     * 修改者
     */
    private Integer updateBy;

    /**
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
