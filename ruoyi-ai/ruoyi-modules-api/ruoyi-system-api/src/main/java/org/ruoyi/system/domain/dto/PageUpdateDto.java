package org.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 单页 Data JSON 更新请求
 */
@Data
public class PageUpdateDto {

    /** 页面 ID */
    @NotNull(message = "页面ID不能为空")
    private Integer pageId;

    /** 修改后的 Data JSON（image slot 值为 Map，text slot 值为 String） */
    @NotNull(message = "数据不能为空")
    private Map<String, Object> dataMap;
}
