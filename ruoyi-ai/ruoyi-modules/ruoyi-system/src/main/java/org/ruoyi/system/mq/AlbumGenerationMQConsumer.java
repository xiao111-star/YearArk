package org.ruoyi.system.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.system.config.RabbitMQConfig;
import org.ruoyi.system.domain.YaAlbum;
import org.ruoyi.system.domain.YaAlbumPage;
import org.ruoyi.system.domain.YaTemplatePage;
import org.ruoyi.system.domain.dto.ValidationResult;
import org.ruoyi.system.domain.mq.GenerationResultMessage;
import org.ruoyi.system.service.IYaAlbumPageService;
import org.ruoyi.system.service.IYaAlbumService;
import org.ruoyi.system.service.IYaTemplatePageService;
import org.ruoyi.system.service.IYaTemplateSchemaService;
import org.ruoyi.system.util.SchemaValidatorUtil;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 纪念册生成结果 MQ 消费组件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumGenerationMQConsumer {

    private final IYaAlbumService albumService;
    private final IYaAlbumPageService albumPageService;
    private final IYaTemplatePageService templatePageService;
    private final IYaTemplateSchemaService templateSchemaService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @RabbitListener(queues = RabbitMQConfig.RESULT_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void onResult(String message) {
        log.info("[MQ收到消息] queue={} body={}", RabbitMQConfig.RESULT_QUEUE,
                message.length() > 500 ? message.substring(0, 500) + "..." : message);
        GenerationResultMessage result;
        try {
            result = OBJECT_MAPPER.readValue(message, GenerationResultMessage.class);
        } catch (Exception e) {
            log.error("解析生成结果消息失败: {}", e.getMessage(), e);
            return;
        }

        log.info("收到生成结果 correlationId={} albumId={} status={}",
                result.getCorrelationId(), result.getAlbumId(), result.getStatus());

        Integer albumId = result.getAlbumId();

        // Python 服务返回失败
        if (!"success".equals(result.getStatus())) {
            markFailed(albumId, result.getErrorMessage() != null
                    ? result.getErrorMessage() : "AI 服务生成失败");
            return;
        }

        if (result.getPages() == null || result.getPages().isEmpty()) {
            markFailed(albumId, "AI 服务未返回任何页面数据");
            return;
        }

        // 逐页 Schema 校验
        List<String> validationErrors = new ArrayList<>();
        List<YaAlbumPage> pagesToSave = new ArrayList<>();

        for (int i = 0; i < result.getPages().size(); i++) {
            GenerationResultMessage.PageDataItem pageData = result.getPages().get(i);

            YaTemplatePage templatePage = templatePageService.getById(pageData.getTemplatePageId());
            if (templatePage == null) {
                validationErrors.add("模板页 [id=" + pageData.getTemplatePageId() + "] 不存在");
                continue;
            }

            String schemaContent = null;
            if (templatePage.getTemplateSchemaId() != null) {
                var schema = templateSchemaService.getById(templatePage.getTemplateSchemaId());
                if (schema != null) {
                    schemaContent = schema.getContent();
                }
            }

            Map<String, Object> dataMap = new HashMap<>(pageData.getDataMap());
            ValidationResult vr = SchemaValidatorUtil.validate(dataMap, schemaContent);
            if (!vr.isValid()) {
                vr.getErrors().forEach(e ->
                        validationErrors.add("页面[templatePageId=" + pageData.getTemplatePageId()
                                + "] slot[" + e.getSlotId() + "]: " + e.getMessage()));
                continue;
            }

            try {
                String dataJson = OBJECT_MAPPER.writeValueAsString(dataMap);
                YaAlbumPage page = new YaAlbumPage();
                page.setAlbumId(albumId);
                page.setTemplatePageId(pageData.getTemplatePageId());
                page.setData(dataJson);
                page.setSort(i + 1);
                page.setIsDelete(0);
                pagesToSave.add(page);
            } catch (Exception e) {
                validationErrors.add("序列化页面数据失败: " + e.getMessage());
            }
        }

        if (!validationErrors.isEmpty()) {
            markFailed(albumId, String.join("; ", validationErrors));
            return;
        }

        // 清除旧页面，批量插入新页面
        albumPageService.remove(
                new LambdaQueryWrapper<YaAlbumPage>().eq(YaAlbumPage::getAlbumId, albumId)
        );
        albumPageService.saveBatch(pagesToSave);

        // 更新状态为 completed
        YaAlbum update = new YaAlbum();
        update.setId(albumId);
        update.setGenerationStatus(2);
        update.setGenerationFailReason(null);
        albumService.updateById(update);

        log.info("纪念册 [albumId={}] 生成完成，共 {} 页", albumId, pagesToSave.size());
    }

    private void markFailed(Integer albumId, String reason) {
        log.warn("纪念册 [albumId={}] 生成失败: {}", albumId, reason);
        YaAlbum update = new YaAlbum();
        update.setId(albumId);
        update.setGenerationStatus(3);
        update.setGenerationFailReason(reason);
        albumService.updateById(update);
    }
}
