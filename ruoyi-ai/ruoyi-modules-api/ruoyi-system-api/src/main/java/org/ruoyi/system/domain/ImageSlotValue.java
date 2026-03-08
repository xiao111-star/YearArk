package org.ruoyi.system.domain;

import lombok.Data;

import java.util.Map;

/**
 * image slot 值对象，兼容纯字符串 URL 和对象格式 {url, focus_x, focus_y, scale}
 */
@Data
public class ImageSlotValue {

    /** 图片 URL */
    private String url;

    /** 焦点 X 坐标（0.0~1.0），默认居中 */
    private Double focusX = 0.5;

    /** 焦点 Y 坐标（0.0~1.0），默认居中 */
    private Double focusY = 0.5;

    /** 缩放倍率，1.0 为刚好 cover */
    private Double scale = 1.0;

    /**
     * 从 Data JSON 中的值解析 ImageSlotValue
     * 兼容纯字符串 URL 和 Map 对象格式
     */
    public static ImageSlotValue fromDataValue(Object value) {
        if (value instanceof String url) {
            ImageSlotValue v = new ImageSlotValue();
            v.setUrl(url);
            return v;
        }
        if (value instanceof Map<?, ?> map) {
            ImageSlotValue v = new ImageSlotValue();
            v.setUrl(String.valueOf(map.get("url")));
            v.setFocusX(parseDouble(map.get("focus_x"), 0.5));
            v.setFocusY(parseDouble(map.get("focus_y"), 0.5));
            v.setScale(parseDouble(map.get("scale"), 1.0));
            return v;
        }
        return null;
    }

    private static Double parseDouble(Object val, double defaultVal) {
        if (val instanceof Number n) return n.doubleValue();
        return defaultVal;
    }
}
