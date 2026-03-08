package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.system.domain.ImageSlotValue;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模板渲染服务实现（支持 focus_point + scale）
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
            return htmlTemplate.replaceAll("\\{\\{[^}]+\\}\\}", "");
        }

        Map<String, Object> data;
        try {
            data = OBJECT_MAPPER.readValue(dataJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("解析 dataJson 失败: {}", e.getMessage());
            return htmlTemplate.replaceAll("\\{\\{[^}]+\\}\\}", "");
        }

        String result = htmlTemplate;
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith("image_")) {
                result = renderImageSlot(result, key, value);
            } else {
                // text slot：直接替换占位符
                String text = value != null ? String.valueOf(value) : "";
                result = result.replace("{{" + key + "}}", text);
            }
        }

        return result.replaceAll("\\{\\{[^}]+\\}\\}", "");
    }

    /**
     * 渲染 image slot：替换占位符并注入 focus_point + scale 样式
     */
    private String renderImageSlot(String html, String key, Object value) {
        ImageSlotValue slot = ImageSlotValue.fromDataValue(value);
        if (slot == null || slot.getUrl() == null) {
            return html.replace("{{" + key + "}}", "");
        }

        // 替换 URL 占位符
        String result = html.replace("{{" + key + "}}", slot.getUrl());

        // 注入 style 到对应 <img> 标签（匹配含有该 URL 的 img 标签）
        String focusStyle = buildFocusStyle(slot);
        result = injectImgStyle(result, slot.getUrl(), focusStyle);

        return result;
    }

    /**
     * 构建 focus_point + scale 的 CSS style 字符串
     */
    private String buildFocusStyle(ImageSlotValue slot) {
        double fx = slot.getFocusX() * 100;
        double fy = slot.getFocusY() * 100;
        double scale = slot.getScale();
        return String.format(
                "object-fit:cover;object-position:%.1f%% %.1f%%;transform:scale(%.2f);transform-origin:%.1f%% %.1f%%",
                fx, fy, scale, fx, fy
        );
    }

    /**
     * 在 HTML 中找到 src 等于指定 URL 的 img 标签，追加或合并 style 属性
     */
    private String injectImgStyle(String html, String url, String style) {
        String escapedUrl = Pattern.quote(url);
        Pattern imgPattern = Pattern.compile("(<img\\b[^>]*src=[\"']" + escapedUrl + "[\"'][^>]*)(>)");
        Matcher matcher = imgPattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String imgTag = matcher.group(1);
            if (imgTag.contains("style=")) {
                // 追加到已有 style
                imgTag = imgTag.replaceFirst("(style=[\"'])([^\"']*)", "$1$2;" + Matcher.quoteReplacement(style));
            } else {
                imgTag = imgTag + " style=\"" + style + "\"";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(imgTag + ">"));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public List<RenderedPageVo> renderAlbum(Integer albumId) {
        List<YaAlbumPage> albumPages = albumPageService.list(
                new LambdaQueryWrapper<YaAlbumPage>()
                        .eq(YaAlbumPage::getAlbumId, albumId)
                        .orderByAsc(YaAlbumPage::getSort)
        );

        if (albumPages == null || albumPages.isEmpty()) {
            return Collections.emptyList();
        }

        List<RenderedPageVo> result = new ArrayList<>();
        for (YaAlbumPage albumPage : albumPages) {
            YaTemplatePage templatePage = templatePageService.getById(albumPage.getTemplatePageId());
            if (templatePage == null) {
                log.warn("album_page [id={}] 关联的 template_page [id={}] 不存在，跳过",
                        albumPage.getId(), albumPage.getTemplatePageId());
                continue;
            }
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
