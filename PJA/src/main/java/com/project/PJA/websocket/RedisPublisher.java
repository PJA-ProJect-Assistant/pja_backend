package com.project.PJA.websocket;

import com.google.gson.Gson;
import com.project.PJA.websocket.dto.ApiUpdateMessage;
import com.project.PJA.websocket.dto.IdeaUpdateMessage;
import com.project.PJA.websocket.dto.RequirementUpdateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

// Redis Pub/Sub 사용해 메시지를 퍼뜨림
@Service
@RequiredArgsConstructor
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic topic;
    private final Gson gson = new Gson();

    public void publishIdea(IdeaUpdateMessage message) {
        String json = gson.toJson(message);
        redisTemplate.convertAndSend(topic.getTopic(), json);
    }

    public void publishRequirement(RequirementUpdateMessage message) {
        String json = gson.toJson(message);
        redisTemplate.convertAndSend(topic.getTopic(), json);
    }

    public void publishApi(ApiUpdateMessage message) {
        String json = gson.toJson(message);
        redisTemplate.convertAndSend(topic.getTopic(), json);
    }
}
