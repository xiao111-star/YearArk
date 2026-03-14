package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 编辑模式下的页面数据（含 Data JSON 和 Schema）
 */
@Data
public class EditablePageVo {

    /** 页面 ID */
    private Integer pageId;

    /** 页面排序 */
    private Integer sort;

    /** 渲染后的 HTML */
    private String html;

    /** Data JSON（image slot 值为 Map，text slot 值为 String） */
    private Map<String, Object> data;

    /** Schema JSON 字符串（原始内容，前端解析 slots 定义） */
    private String schemaContent;
    /** 模板原始 HTML（含 {{slot_id}} 占位符） */
    private String templateHtml;
}
