package org.ruoyi.system.domain.mq;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 纪念册生成结果 MQ 消息（Python → Java）
 */
@Data
public class GenerationResultMessage {

    /** 请求唯一标识，与请求消息的 correlationId 对应 */
    private String correlationId;

    /** 纪念册 ID */
    private Integer albumId;

    /** 生成状态：success / failed */
    private String status;

    /** 每页的生成结果（status=success 时有值） */
    private List<PageDataItem> pages;

    /** 错误信息（status=failed 时有值） */
    private String errorMessage;

    @Data
    public static class PageDataItem {
        /** 模板页 ID */
        private Integer templatePageId;
        /**
         * Data JSON 扁平 key-value 结构
         * image slot 值为 Map {url, focus_x, focus_y, scale}
         * text slot 值为 String
         */
        private Map<String, Object> dataMap;
    }
}
