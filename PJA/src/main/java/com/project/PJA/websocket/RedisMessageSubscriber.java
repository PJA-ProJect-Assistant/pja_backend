package com.project.PJA.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.project.PJA.websocket.dto.ApiUpdateMessage;
import com.project.PJA.websocket.dto.IdeaUpdateMessage;
import com.project.PJA.websocket.dto.RequirementUpdateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

// 메시지를 받아 다시 WebSocket으로 브로드캐스트
// 서버에 등록된 RedisMessageSubscriber가 Redis 메시지를 수신
@Component
@RequiredArgsConstructor
public class RedisMessageSubscriber implements MessageListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final Gson gson = new Gson();

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        JsonObject jsonObject = JsonParser.parseString(payload).getAsJsonObject();
        String type = jsonObject.get("type").getAsString();
        Long workspaceId = jsonObject.get("workspaceId").getAsLong();

        // Redis에서 받은 메시지를 객체로 변환
        //IdeaUpdateMessage updateMessage = gson.fromJson(payload, IdeaUpdateMessage.class);

        // 해당 workspace에 연결된 클라이언트에게 실시간 전송
        //messagingTemplate.convertAndSend("/topic/workspace/" + updateMessage.getWorkspaceId(), updateMessage);

        switch (type) {
            case "idea" -> {
                IdeaUpdateMessage ideaMessage = gson.fromJson(payload, IdeaUpdateMessage.class);

                // 해당 WebSocket 채널(/topic/workspace/{workspaceId}) 을 구독 중인 모든 사용자에게 ideaMessage가 자동으로 전송
                messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, ideaMessage);
            }
            case "requirement" -> {
                RequirementUpdateMessage reqMessage = gson.fromJson(payload, RequirementUpdateMessage.class);
                messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, reqMessage);
            }
            case "api" -> {
                ApiUpdateMessage apiMessage = gson.fromJson(payload, ApiUpdateMessage.class);
                messagingTemplate.convertAndSend("/topic/workspace/" + workspaceId, apiMessage);
            }
        }
    }
}
