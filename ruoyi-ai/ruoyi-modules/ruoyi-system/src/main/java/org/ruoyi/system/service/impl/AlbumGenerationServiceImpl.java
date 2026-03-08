package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.DictService;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaAlbumMedia;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.YaTemplateSchema;
import org.ruoyi.system.domain.item.MediaItem;
import org.ruoyi.system.domain.item.TemplatePageItem;
import org.ruoyi.system.domain.mq.GenerationRequestMessage;
import org.ruoyi.system.mq.AlbumGenerationMQPublisher;
import org.ruoyi.system.service.AlbumGenerationService;
import org.ruoyi.system.service.IYaAlbumMediaService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 纪念册生成服务实现（MQ 版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlbumGenerationServiceImpl implements AlbumGenerationService {

    private final IYaAlbumService albumService;
    private final IYaAlbumMediaService albumMediaService;
    private final IYaTemplatePageService templatePageService;
    private final IYaTemplateSchemaService templateSchemaService;
    private final AlbumGenerationMQPublisher mqPublisher;
    private final DictService dictService;

    @Override
    public R<Void> generate(Integer albumId) {
        // 1. 校验纪念册存在且已关联模板
        YaAlbum album = albumService.getById(albumId);
        if (album == null) {
            throw new ServiceException("纪念册不存在");
        }
        if (album.getTemplateId() == null) {
            throw new ServiceException("请先选择模板");
        }

        // 2. 校验至少有 1 条 status=2 的素材
        long approvedCount = albumMediaService.count(
                new LambdaQueryWrapper<YaAlbumMedia>()
                        .eq(YaAlbumMedia::getAlbumId, albumId)
                        .eq(YaAlbumMedia::getStatus, 2)
        );
        if (approvedCount == 0) {
            throw new ServiceException("请先上传素材");
        }

        // 3. 更新状态为 processing
        YaAlbum statusUpdate = new YaAlbum();
        statusUpdate.setId(albumId);
        statusUpdate.setGenerationStatus(1);
        statusUpdate.setGenerationFailReason(null);
        albumService.updateById(statusUpdate);

        // 4. 查询素材列表（status=2，按 sort 排序）
        List<YaAlbumMedia> mediaList = albumMediaService.list(
                new LambdaQueryWrapper<YaAlbumMedia>()
                        .eq(YaAlbumMedia::getAlbumId, albumId)
                        .eq(YaAlbumMedia::getStatus, 2)
                        .orderByAsc(YaAlbumMedia::getSort)
        );

        // 5. 查询模板页列表（按 id 排序）
        List<YaTemplatePage> templatePages = templatePageService.list(
                new LambdaQueryWrapper<YaTemplatePage>()
                        .eq(YaTemplatePage::getTemplateId, album.getTemplateId())
                        .orderByAsc(YaTemplatePage::getId)
        );
        if (templatePages == null || templatePages.isEmpty()) {
            throw new ServiceException("模板配置异常，请联系管理员");
        }

        // 6. 组装 MQ 请求消息
        GenerationRequestMessage message = new GenerationRequestMessage();
        message.setCorrelationId(UUID.randomUUID().toString());
        message.setAlbumId(albumId);

        List<MediaItem> mqMediaList = mediaList.stream().map(m -> {
            MediaItem item = new MediaItem();
            item.setId(m.getId());
            item.setType(m.getType());
            item.setContent(m.getContent());
            item.setSort(m.getSort() != null ? m.getSort() : 0);
            return item;
        }).collect(Collectors.toList());
        message.setMediaList(mqMediaList);

        List<TemplatePageItem> mqTemplatePages = templatePages.stream().map(tp -> {
            YaTemplateSchema schema = templateSchemaService.getById(tp.getTemplateSchemaId());
            TemplatePageItem item = new TemplatePageItem();
            item.setTemplatePageId(tp.getId());
            item.setSchemaId(tp.getTemplateSchemaId() != null ? tp.getTemplateSchemaId() : 0);
            item.setImageCount(schema != null && schema.getImageCount() != null ? schema.getImageCount() : 0);
            item.setTextCount(schema != null && schema.getTextCount() != null ? schema.getTextCount() : 0);
            item.setPageTypeName(dictService.getDictLabel("ya_template_page_type", String.valueOf(tp.getType())));
            item.setSchemaContent(schema != null ? schema.getContent() : null);
            return item;
        }).collect(Collectors.toList());
        message.setTemplatePages(mqTemplatePages);

        // 7. 发送到 MQ，由 Python AI 服务处理，结果由 AlbumGenerationMQConsumer 接收
        mqPublisher.publish(message);

        return R.ok();
    }
}
