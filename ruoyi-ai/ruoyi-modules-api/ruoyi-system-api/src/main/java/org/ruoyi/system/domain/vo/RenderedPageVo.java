package org.ruoyi.system.domain.vo;

import lombok.Data;

/**
 * 渲染后的纪念册页面 VO
 *
 * @author YearArk
 */
@Data
public class RenderedPageVo {

    private Integer pageId;

    private Integer sort;

    /** 渲染后的完整 HTML */
    private String html;
}
