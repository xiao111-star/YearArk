package org.ruoyi.system.service;

import org.ruoyi.system.domain.vo.RenderedPageVo;

import java.util.List;

/**
 * 模板渲染服务
 *
 * @author YearArk
 */
public interface TemplateRenderService {

    /**
     * 渲染单页：将 HTML 模板中的 {{slot_id}} 替换为 Data JSON 中的值
     *
     * @param htmlTemplate HTML 模板字符串
     * @param dataJson     Data JSON 字符串
     * @return 渲染后的 HTML
     */
    String renderPage(String htmlTemplate, String dataJson);

    /**
     * 渲染整本纪念册：返回所有页面的渲染后 HTML 列表
     *
     * @param albumId 纪念册ID
     * @return 渲染后的页面列表
     */
    List<RenderedPageVo> renderAlbum(Integer albumId);
}
