package com.project.PJA.security.config;

import com.project.PJA.websocket.RedisMessageSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private final RedisMessageSubscriber redisMessageSubscriber;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }

    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        //container.addMessageListener(redisMessageSubscriber, new PatternTopic("workspace-update"));
        // 각각의 Redis 채널에 대해 리스너 등록
        container.addMessageListener(redisMessageSubscriber, new PatternTopic("idea-update"));
        container.addMessageListener(redisMessageSubscriber, new PatternTopic("requirement-update"));
        container.addMessageListener(redisMessageSubscriber, new PatternTopic("api-update"));

        return container;
    }

    // Redis 채널 이름
    /*@Bean
    public ChannelTopic topic() {
        return new ChannelTopic("workspace-update");
    }*/

    @Bean
    public ChannelTopic ideaTopic() {
        return new ChannelTopic("idea-update");
    }

    @Bean
    public ChannelTopic requirementTopic() {
        return new ChannelTopic("requirement-update");
    }

    @Bean
    public ChannelTopic apiTopic() {
        return new ChannelTopic("api-update");
    }
}
