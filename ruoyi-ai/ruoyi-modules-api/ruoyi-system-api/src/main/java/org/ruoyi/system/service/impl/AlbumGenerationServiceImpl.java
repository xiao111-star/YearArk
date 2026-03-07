package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.YaTemplateSchema;
import org.ruoyi.system.service.AlbumGenerationService;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 纪念册生成服务实现
 *
 * @author YearArk
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumGenerationServiceImpl implements AlbumGenerationService {

    private final IYaAlbumService albumService;
    private final IYaAlbumMediaService albumMediaService;
    private final IYaAlbumPageService albumPageService;
    private final IYaTemplatePageService templatePageService;
    private final IYaTemplateSchemaService templateSchemaService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 默认占位图 URL */
    private static final String DEFAULT_PLACEHOLDER_IMAGE = "https://placeholder.com/default.jpg";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> generate(Integer albumId) {
        // a. 验证纪念册存在并已关联模板
        YaAlbum album = albumService.getById(albumId);
        if (album == null) {
            throw new ServiceException("纪念册不存在");
        }
        if (album.getTemplateId() == null) {
            throw new ServiceException("请先选择模板");
        }

        // b. 验证至少有 1 条 status=2 的素材
        long approvedCount = albumMediaService.count(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, albumId)
                .eq(YaAlbumMedia::getStatus, 2)
        );
        if (approvedCount == 0) {
            throw new ServiceException("请先上传素材");
        }

        // c. 读取模板的所有 TemplatePage（按 id 排序）
        List<YaTemplatePage> templatePages = templatePageService.list(
            new LambdaQueryWrapper<YaTemplatePage>()
                .eq(YaTemplatePage::getTemplateId, album.getTemplateId())
                .orderByAsc(YaTemplatePage::getId)
        );
        if (templatePages == null || templatePages.isEmpty()) {
            throw new ServiceException("模板配置异常，请联系管理员");
        }

        // d. 收集图片素材（type=2, status=2）和文字素材（type=1, status=2），按 sort 排序
        List<YaAlbumMedia> images = albumMediaService.list(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, albumId)
                .eq(YaAlbumMedia::getType, 2)
                .eq(YaAlbumMedia::getStatus, 2)
                .orderByAsc(YaAlbumMedia::getSort)
        );
        List<YaAlbumMedia> texts = albumMediaService.list(
            new LambdaQueryWrapper<YaAlbumMedia>()
                .eq(YaAlbumMedia::getAlbumId, albumId)
                .eq(YaAlbumMedia::getType, 1)
                .eq(YaAlbumMedia::getStatus, 2)
                .orderByAsc(YaAlbumMedia::getSort)
        );

        // e. 生成前先清除已有的 album_page 记录（支持重新生成）
        albumPageService.remove(
            new LambdaQueryWrapper<YaAlbumPage>()
                .eq(YaAlbumPage::getAlbumId, albumId)
        );

        // f. 按素材分配算法，逐页分配素材生成 Data JSON
        int imageIndex = 0;
        int textIndex = 0;
        int pageSort = 1;
        List<YaAlbumPage> pagesToSave = new ArrayList<>();

        for (YaTemplatePage templatePage : templatePages) {
            // 获取关联的 Schema
            YaTemplateSchema schema = templateSchemaService.getById(templatePage.getTemplateSchemaId());
            if (schema == null) {
                log.warn("模板页 [id={}] 关联的 Schema [id={}] 不存在，跳过",
                    templatePage.getId(), templatePage.getTemplateSchemaId());
                continue;
            }

            int imageCount = schema.getImageCount() != null ? schema.getImageCount() : 0;
            int textCount = schema.getTextCount() != null ? schema.getTextCount() : 0;

            Map<String, String> dataMap = new LinkedHashMap<>();

            // 填充图片
            for (int i = 1; i <= imageCount; i++) {
                if (imageIndex < images.size()) {
                    dataMap.put("image_" + i, images.get(imageIndex).getContent());
                    imageIndex++;
                } else {
                    dataMap.put("image_" + i, DEFAULT_PLACEHOLDER_IMAGE);
                }
            }

            // 填充文字
            for (int i = 1; i <= textCount; i++) {
                if (textIndex < texts.size()) {
                    dataMap.put("text_" + i, texts.get(textIndex).getContent());
                    textIndex++;
                } else {
                    dataMap.put("text_" + i, "");
                }
            }

            // g. 为每页创建 ya_album_page 记录
            String dataJson;
            try {
                dataJson = OBJECT_MAPPER.writeValueAsString(dataMap);
            } catch (JsonProcessingException e) {
                log.error("序列化 Data JSON 失败: {}", e.getMessage());
                throw new ServiceException("生成纪念册失败，数据序列化异常");
            }

            YaAlbumPage albumPage = new YaAlbumPage();
            albumPage.setAlbumId(albumId);
            albumPage.setTemplatePageId(templatePage.getId());
            albumPage.setData(dataJson);
            albumPage.setSort(pageSort++);
            albumPage.setIsDelete(0);
            albumPage.setCreateAt(LocalDateTime.now());
            albumPage.setUpdateAt(LocalDateTime.now());
            pagesToSave.add(albumPage);
        }

        // 批量保存 album_page
        if (!pagesToSave.isEmpty()) {
            albumPageService.saveBatch(pagesToSave);
        }

        // h. 更新 ya_album.status = 1（发布）
        YaAlbum updateAlbum = new YaAlbum();
        updateAlbum.setId(albumId);
        updateAlbum.setStatus(1);
        albumService.updateById(updateAlbum);

        return R.ok("纪念册生成成功");
    }
}
