package com.project.PJA.sse.service;

import com.project.PJA.sse.repository.SseEmitterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {
    private final SseEmitterRepository sseEmitterRepository;

    @Async
    public void notifyWorkspaceChange(Long workspaceId, String page) {
        Map<String, SseEmitter> emitters = sseEmitterRepository.getPage(workspaceId, page);
        log.info("워크스페이스 변경 알림 시작 - workspaceId: {}, page: {}", workspaceId, page);

        for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
            String emitterId = entry.getKey();
            SseEmitter emitter = entry.getValue();

            try {
                emitter.send(SseEmitter.event()
                        .name("page-update")
                        .data("변경됨")
                        .reconnectTime(10_000L));
            } catch (IOException e) {
                log.error("SSE 전송 실패 emitterId : {}", emitterId, e);
            } finally {
                Long userId = extractUserId(emitterId);
                sseEmitterRepository.deletePage(workspaceId, page, userId);
            }
        }
    }

    private Long extractUserId(String emitterId) {
        String[] parts = emitterId.split(":");
        return Long.parseLong(parts[2]);
    }
}
