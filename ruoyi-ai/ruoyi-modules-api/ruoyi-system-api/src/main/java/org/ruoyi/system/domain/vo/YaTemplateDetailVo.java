package org.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 模板详情 VO（含模板基本信息 + 模板页列表）
 *
 * @author YearArk
 */
@Data
public class YaTemplateDetailVo {

    private YaTemplateVo template;

    private List<YaTemplatePageVo> pages;
}
