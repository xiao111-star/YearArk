package org.ruoyi.system.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.system.domain.dto.SlotError;
import org.ruoyi.system.domain.dto.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schema 校验工具类
 * 解析 Schema JSON 中的 slots 数组，逐个 slot 校验 Data JSON
 */
@Slf4j
public class SchemaValidatorUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SchemaValidatorUtil() {}

    public static ValidationResult validate(Map<String, Object> dataMap, String schemaContent) {
        if (schemaContent == null || schemaContent.isBlank()) {
            return ValidationResult.ok();
        }

        List<Map<String, Object>> slots;
        try {
            Map<String, Object> schema = OBJECT_MAPPER.readValue(schemaContent, new TypeReference<>() {});
            Object slotsObj = schema.get("slots");
            if (!(slotsObj instanceof List)) {
                return ValidationResult.ok();
            }
            //noinspection unchecked
            slots = (List<Map<String, Object>>) slotsObj;
        } catch (Exception e) {
            log.warn("解析 Schema JSON 失败: {}", e.getMessage());
            return ValidationResult.ok();
        }

        List<SlotError> errors = new ArrayList<>();

        for (Map<String, Object> slot : slots) {
            String id = String.valueOf(slot.get("id"));
            String type = String.valueOf(slot.get("type"));
            boolean required = Boolean.TRUE.equals(slot.get("required"));
            Object defaultVal = slot.get("default");

            Object value = dataMap.get(id);

            // 缺失处理：非必填且有 default 值时自动填充
            if (value == null || (value instanceof String s && s.isBlank())) {
                if (required) {
                    errors.add(new SlotError(id, "必填项不能为空"));
                    continue;
                }
                if (defaultVal != null) {
                    dataMap.put(id, defaultVal);
                }
                continue;
            }

            // text slot 校验
            if ("text".equals(type)) {
                String text = String.valueOf(value);
                Object maxLengthObj = slot.get("maxLength");
                if (maxLengthObj instanceof Number maxLength) {
                    if (text.length() > maxLength.intValue()) {
                        errors.add(new SlotError(id, "文字长度超过最大限制" + maxLength.intValue()));
                    }
                }
            }

            // image slot 校验
            if ("image".equals(type)) {
                String url = extractUrl(value);
                if (url == null || url.isBlank()) {
                    errors.add(new SlotError(id, "图片 URL 不能为空"));
                } else if (!isValidUrl(url)) {
                    errors.add(new SlotError(id, "图片 URL 格式不合法"));
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.ok() : ValidationResult.fail(errors);
    }

    private static String extractUrl(Object value) {
        if (value instanceof String s) return s;
        if (value instanceof Map<?, ?> map) {
            Object url = map.get("url");
            return url != null ? String.valueOf(url) : null;
        }
        return null;
    }

    private static boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
