package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.vo.RenderedPageVo;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.TemplateRenderService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 模板渲染服务实现
 *
 * @author YearArk
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateRenderServiceImpl implements TemplateRenderService {

    private final IYaAlbumPageService albumPageService;
    private final IYaTemplatePageService templatePageService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String renderPage(String htmlTemplate, String dataJson) {
        if (htmlTemplate == null || htmlTemplate.isBlank()) {
            return "";
        }
        if (dataJson == null || dataJson.isBlank()) {
            // 没有数据，清除所有占位符
            return htmlTemplate.replaceAll("\\{\\{[^}]+\\}\\}", "");
        }

        Map<String, String> data;
        try {
            data = OBJECT_MAPPER.readValue(dataJson, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析 dataJson 失败: {}", e.getMessage());
            // JSON 解析失败，清除所有占位符返回
            return htmlTemplate.replaceAll("\\{\\{[^}]+\\}\\}", "");
        }

        String result = htmlTemplate;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue() : "");
        }
        // 清除未匹配的占位符
        result = result.replaceAll("\\{\\{[^}]+\\}\\}", "");
        return result;
    }

    @Override
    public List<RenderedPageVo> renderAlbum(Integer albumId) {
        // 1. 查询所有 album_page，按 sort 排序
        List<YaAlbumPage> albumPages = albumPageService.list(
                new LambdaQueryWrapper<YaAlbumPage>()
                        .eq(YaAlbumPage::getAlbumId, albumId)
                        .orderByAsc(YaAlbumPage::getSort)
        );

        if (albumPages == null || albumPages.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 逐页渲染
        List<RenderedPageVo> result = new ArrayList<>();
        for (YaAlbumPage albumPage : albumPages) {
            // 关联查询 template_page 获取 HTML 模板
            YaTemplatePage templatePage = templatePageService.getById(albumPage.getTemplatePageId());
            if (templatePage == null) {
                log.warn("album_page [id={}] 关联的 template_page [id={}] 不存在，跳过",
                        albumPage.getId(), albumPage.getTemplatePageId());
                continue;
            }

            // 调用 renderPage 渲染
            String html = renderPage(templatePage.getContent(), albumPage.getData());

            RenderedPageVo vo = new RenderedPageVo();
            vo.setPageId(albumPage.getId());
            vo.setSort(albumPage.getSort());
            vo.setHtml(html);
            result.add(vo);
        }

        return result;
    }
}
