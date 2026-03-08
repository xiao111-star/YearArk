package org.ruoyi.system.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema 校验结果
 */
@Data
public class ValidationResult {

    /** 是否校验通过 */
    private boolean valid;

    /** 校验失败的 slot 列表 */
    private List<SlotError> errors = new ArrayList<>();

    public static ValidationResult ok() {
        ValidationResult r = new ValidationResult();
        r.setValid(true);
        return r;
    }

    public static ValidationResult fail(List<SlotError> errors) {
        ValidationResult r = new ValidationResult();
        r.setValid(false);
        r.setErrors(errors);
        return r;
    }
}
