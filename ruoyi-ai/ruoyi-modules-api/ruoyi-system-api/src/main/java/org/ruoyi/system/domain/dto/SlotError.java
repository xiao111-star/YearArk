package org.ruoyi.system.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Schema 校验失败的单个 slot 错误信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotError {

    /** slot ID（如 image_1、text_1） */
    private String slotId;

    /** 失败原因描述 */
    private String message;
}
