package com.project.PJA.sse.repository;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class SseEmitterRepository {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter save(Long workspaceId, Long userId, SseEmitter emitter) {
        String emitterId = getEmitterId(workspaceId, userId);
        emitters.put(emitterId, emitter);
        return emitter;
    }

    public SseEmitter savePage(Long workspaceId, String page, Long userId, SseEmitter emitter) {
        String emitterId = getPageEmitterId(workspaceId, page, userId);
        emitters.put(emitterId, emitter);
        return emitter;
    }

    public Optional<SseEmitter> get(Long workspaceId, Long userId) {
        String emitterId = getEmitterId(workspaceId, userId);
        return Optional.ofNullable(emitters.get(emitterId));
    }

    public Map<String, SseEmitter> getPage(Long workspaceId, String page) {
        String prefix = workspaceId + ":" + page + ":";
        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void delete(Long workspaceId, Long userId) {
        String emitterId = getEmitterId(workspaceId, userId);
        emitters.remove(emitterId);
    }

    public void deletePage(Long workspaceId, String page, Long userId) {
        String emitterId = getPageEmitterId(workspaceId, page, userId);
        emitters.remove(emitterId);
    }

    private String getEmitterId(Long workspaceId, Long userId) {
        return workspaceId + ":" + userId;
    }

    private String getPageEmitterId(Long workspaceId, String page, Long userId) {
        return workspaceId + ":" + page + ":" + userId;
    }
}
