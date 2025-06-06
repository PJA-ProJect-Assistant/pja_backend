package com.project.PJA.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TempController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/")
    public String home() {
        System.out.println(">>> RedisTemplate is null? " + (redisTemplate == null));
        return ">>> RedisTemplate is null? " + (redisTemplate == null);
    }
}
