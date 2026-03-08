package org.ruoyi.system.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ Exchange / Queue 配置
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "yearark.album";
    public static final String REQUEST_QUEUE = "album.generation.request";
    public static final String RESULT_QUEUE = "album.generation.result";

    /** 消息 TTL：5 分钟 */
    private static final int MESSAGE_TTL_MS = 5 * 60 * 1000;

    @Bean
    public DirectExchange albumExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME).durable(true).build();
    }

    @Bean
    public Queue requestQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", MESSAGE_TTL_MS);
        return new Queue(REQUEST_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue resultQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", MESSAGE_TTL_MS);
        return new Queue(RESULT_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding requestBinding(Queue requestQueue, DirectExchange albumExchange) {
        return BindingBuilder.bind(requestQueue).to(albumExchange).with(REQUEST_QUEUE);
    }

    @Bean
    public Binding resultBinding(Queue resultQueue, DirectExchange albumExchange) {
        return BindingBuilder.bind(resultQueue).to(albumExchange).with(RESULT_QUEUE);
    }
}
