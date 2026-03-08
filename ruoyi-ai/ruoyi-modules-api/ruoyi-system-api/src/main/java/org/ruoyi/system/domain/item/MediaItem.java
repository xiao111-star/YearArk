package org.ruoyi.system.domain.item;

import lombok.Data;

@Data
public class MediaItem {
    /** 素材 ID */
    private Integer id;
    /** 素材类型：2=图片，1=文字 */
    private Integer type;
    /** 素材内容（图片 URL 或文字内容） */
    private String content;
    /** 排序值 */
    private Integer sort;
}
