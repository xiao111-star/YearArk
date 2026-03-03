package org.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 纪念册素材对象 ya_album_media
 *
 * @author YearArk
 */
@Data
@NoArgsConstructor
@TableName("ya_album_media")
public class YaAlbumMedia implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 上传的文件 id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 关联的纪念册 id
     */
    private Integer albumId;

    /**
     * 匿名上传人id
     */
    private Integer tokenId;

    /**
     * 类型（1 文本 2 图片）
     */
    private Integer type;

    /**
     * 内容（文本为文本内容  图片为 OSS URL）
     */
    private String content;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 文件大小 MB 为单位
     */
    private Double size;

    /**
     * ai 识别到的人脸数量
     */
    private Integer facesCount;

    /**
     * ai 分组的标签
     */
    private String tags;

    /**
     * 状态（-1 审核不通过 0 待审核 1 审核中 2 审核通过）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createAt;

    /**
     * 更新时间
     */
    private LocalDateTime updateAt;

    /**
     * 逻辑删除字段（0 存在 2 删除）
     */
    @TableLogic
    private Integer isDelete;

}
