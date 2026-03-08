package org.ruoyi.system.domain.mq;

import lombok.Data;
import org.ruoyi.system.domain.item.MediaItem;
import org.ruoyi.system.domain.item.TemplatePageItem;

import java.util.List;

/**
 * 纪念册生成请求 MQ 消息（Java → Python）
 */
@Data
public class GenerationRequestMessage {

    /** 请求唯一标识，用于结果消息的请求-响应关联 */
    private String correlationId;

    /** 纪念册 ID */
    private Integer albumId;

    /** 素材列表（type=2 图片，type=1 文字，均为 status=2 审核通过） */
    private List<MediaItem> mediaList;

    /** 模板页列表 */
    private List<TemplatePageItem> templatePages;

}
