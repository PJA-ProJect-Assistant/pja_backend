package com.project.PJA.websocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 프론트의 구독 경로
        config.enableSimpleBroker("/topic");

        // 프론트가 백엔드로 메시지 전송할 때 경로
        config.setApplicationDestinationPrefixes("/app");
    }

    // 프론트엔드가 백엔드와 웹소켓 연결을 맺을 수 있도록 엔드포인트를 등록하는 역할
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }
}
