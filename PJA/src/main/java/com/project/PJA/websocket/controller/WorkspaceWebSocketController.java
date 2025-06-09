package com.project.PJA.websocket.controller;

import com.project.PJA.websocket.RedisPublisher;
import com.project.PJA.websocket.dto.ApiUpdateMessage;
import com.project.PJA.websocket.dto.IdeaUpdateMessage;
import com.project.PJA.websocket.dto.RequirementUpdateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WorkspaceWebSocketController {
    private final RedisPublisher redisPublisher;

    /*@MessageMapping("/workspace/update")
    public void handleWorkspaceUpdate(IdeaUpdateMessage message) {
        redisPublisher.publish(message);
    }*/

    @MessageMapping("/idea/update")
    public void handleIdeaUpdate(IdeaUpdateMessage message) {
        redisPublisher.publishIdea(message);  // 메시지 내용이 다르면 RedisPublisher에서 가공해도 됩니다
    }

    @MessageMapping("/requirement/update")
    public void handleRequirementUpdate(RequirementUpdateMessage message) {
        redisPublisher.publishRequirement(message);
    }

    @MessageMapping("/api/update")
    public void handleApiUpdate(ApiUpdateMessage message) {
        redisPublisher.publishApi(message);
    }
}
