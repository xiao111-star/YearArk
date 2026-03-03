package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 纪念册页面对象 ya_album_page
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_album_page")
public class YaAlbumPage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 页面 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联的纪念册 id
     */
    private Integer albumId;

    /**
     * 模板 id
     */
    private Integer templatePageId;

    /**
     * 页面描述
     */
    private String des;

    /**
     * 页面排序
     */
    private Integer sort;

    /**
     * 实际填充的 json
     */
    private String data;

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
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
