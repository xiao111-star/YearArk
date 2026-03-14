package org.ruoyi.system.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.dto.PageUpdateDto;
import org.ruoyi.system.domain.dto.ValidationResult;
import org.ruoyi.system.domain.vo.EditablePageVo;
import org.ruoyi.system.domain.vo.RenderedPageVo;
import org.ruoyi.system.service.AlbumPageEditService;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.ruoyi.system.service.TemplateRenderService;
import org.ruoyi.system.util.SchemaValidatorUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * 纪念册页面编辑保存服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumPageEditServiceImpl implements AlbumPageEditService {

    private final IYaAlbumPageService albumPageService;
    private final IYaTemplatePageService templatePageService;
    private final IYaTemplateSchemaService templateSchemaService;
    private final TemplateRenderService templateRenderService;
    private final IYaAlbumService albumService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public List<EditablePageVo> getEditData(Integer albumId) {
        List<YaAlbumPage> pages = albumPageService.lambdaQuery()
                .eq(YaAlbumPage::getAlbumId, albumId)
                .orderByAsc(YaAlbumPage::getSort)
                .list();

        List<EditablePageVo> result = new ArrayList<>();
        for (YaAlbumPage page : pages) {
            YaTemplatePage tp = templatePageService.getById(page.getTemplatePageId());
            if (tp == null) continue;

            String schemaContent = resolveSchemaContent(page.getTemplatePageId());

            Map<String, Object> dataMap = null;
            if (page.getData() != null) {
                try {
                    dataMap = OBJECT_MAPPER.readValue(page.getData(), new TypeReference<>() {});
                } catch (Exception e) {
                    log.warn("解析页面 data 失败 pageId={}: {}", page.getId(), e.getMessage());
                }
            }

            EditablePageVo vo = new EditablePageVo();
            vo.setPageId(page.getId());
            vo.setSort(page.getSort());
            vo.setHtml(templateRenderService.renderPage(tp.getContent(), page.getData()));
            vo.setTemplateHtml(tp.getContent());
            vo.setData(dataMap);
            vo.setSchemaContent(schemaContent);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RenderedPageVo updatePageData(Integer pageId, Integer userId, Map<String, Object> dataMap) {
        YaAlbumPage page = getPageOrThrow(pageId);
        albumService.checkOwnership(page.getAlbumId(), userId);
        String schemaContent = resolveSchemaContent(page.getTemplatePageId());

        Map<String, Object> mutableData = new HashMap<>(dataMap);
        ValidationResult vr = SchemaValidatorUtil.validate(mutableData, schemaContent);
        if (!vr.isValid()) {
            throw new ServiceException("数据校验失败: " + buildErrorMessage(vr));
        }

        String dataJson = serialize(mutableData);
        page.setData(dataJson);
        albumPageService.updateById(page);

        return renderPage(page, dataJson);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RenderedPageVo> batchUpdatePageData(List<PageUpdateDto> updates) {
        List<RenderedPageVo> results = new ArrayList<>();

        for (PageUpdateDto dto : updates) {
            YaAlbumPage page = getPageOrThrow(dto.getPageId());
            String schemaContent = resolveSchemaContent(page.getTemplatePageId());

            Map<String, Object> mutableData = new HashMap<>(dto.getDataMap());
            ValidationResult vr = SchemaValidatorUtil.validate(mutableData, schemaContent);
            if (!vr.isValid()) {
                // 任一失败，事务回滚
                throw new ServiceException("页面[id=" + dto.getPageId() + "] 校验失败: " + buildErrorMessage(vr));
            }

            String dataJson = serialize(mutableData);
            page.setData(dataJson);
            albumPageService.updateById(page);
            results.add(renderPage(page, dataJson));
        }

        return results;
    }

    // ---- helpers ----

    private YaAlbumPage getPageOrThrow(Integer pageId) {
        YaAlbumPage page = albumPageService.getById(pageId);
        if (page == null) {
            throw new ServiceException("页面不存在");
        }
        return page;
    }

    private String resolveSchemaContent(Integer templatePageId) {
        if (templatePageId == null) return null;
        YaTemplatePage tp = templatePageService.getById(templatePageId);
        if (tp == null || tp.getTemplateSchemaId() == null) return null;
        var schema = templateSchemaService.getById(tp.getTemplateSchemaId());
        return schema != null ? schema.getContent() : null;
    }

    private String serialize(Map<String, Object> data) {
        try {
            return OBJECT_MAPPER.writeValueAsString(data);
        } catch (Exception e) {
            throw new ServiceException("数据序列化失败");
        }
    }

    private RenderedPageVo renderPage(YaAlbumPage page, String dataJson) {
        YaTemplatePage tp = templatePageService.getById(page.getTemplatePageId());
        String html = tp != null
                ? templateRenderService.renderPage(tp.getContent(), dataJson)
                : "";
        RenderedPageVo vo = new RenderedPageVo();
        vo.setPageId(page.getId());
        vo.setSort(page.getSort());
        vo.setHtml(html);
        return vo;
    }

    private String buildErrorMessage(ValidationResult vr) {
        StringBuilder sb = new StringBuilder();
        vr.getErrors().forEach(e -> sb.append("[").append(e.getSlotId()).append("] ").append(e.getMessage()).append("; "));
        return sb.toString();
    }
}
