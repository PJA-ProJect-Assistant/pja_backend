package com.project.PJA.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class CommonService {
    private final RedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    public Object getLockForWorkspace(String page, Long workspaceId) {
        String key = page + ":" + workspaceId;
        return lockMap.computeIfAbsent(key, k -> new Object());
    }

    public void invalidatePageCache(Long workspaceId, String page) {
        redisTemplate.delete("workspace:" + workspaceId + ":" + page);
    }
}
