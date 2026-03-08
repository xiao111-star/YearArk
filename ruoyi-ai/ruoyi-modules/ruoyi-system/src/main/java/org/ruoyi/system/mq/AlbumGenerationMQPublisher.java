package org.ruoyi.system.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.system.config.RabbitMQConfig;
import org.ruoyi.system.domain.mq.GenerationRequestMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 纪念册生成请求 MQ 发布组件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlbumGenerationMQPublisher {

    private final RabbitTemplate rabbitTemplate;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 发送生成请求消息到请求队列
     */
    public void publish(GenerationRequestMessage message) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(message);
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.REQUEST_QUEUE, json);
            log.info("已发送生成请求 correlationId={} albumId={}", message.getCorrelationId(), message.getAlbumId());
        } catch (Exception e) {
            log.error("发送生成请求 MQ 消息失败: {}", e.getMessage(), e);
            throw new ServiceException("发送生成请求失败，请稍后重试");
        }
    }
}
