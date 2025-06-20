package com.project.PJA.sse.controller;

import com.project.PJA.common.dto.SuccessResponse;
import com.project.PJA.security.jwt.JwtTokenProvider;
import com.project.PJA.sse.repository.SseEmitterRepository;
import com.project.PJA.user.entity.Users;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.http.HttpResponse;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workspaces")
public class SseController {

    private final SseEmitterRepository sseEmitterRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/{workspaceId}/noti/subscribe")
    public SseEmitter subscribe(@PathVariable("workspaceId") Long workspaceId,
                                @RequestParam("token") String token) {
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 동안 SSE 연결

        sseEmitterRepository.save(workspaceId, userId, emitter);

        emitter.onCompletion(() -> sseEmitterRepository.delete(workspaceId, userId));
        emitter.onTimeout(() -> sseEmitterRepository.delete(workspaceId, userId));
        emitter.onError((e) -> sseEmitterRepository.delete(workspaceId,userId));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공")
                    .reconnectTime(10_000L));
        } catch (Exception e) {
            throw new RuntimeException("SSE 연결 오류가 발생했습니다.", e);
        }

        return emitter;
    }
}
