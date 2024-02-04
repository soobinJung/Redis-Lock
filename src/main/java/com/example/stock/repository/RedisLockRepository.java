package com.example.stock.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisLockRepository {

    private RedisTemplate<String, String> redisTemplate;

    public RedisLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean lock(Long key){
        return redisTemplate
                .opsForValue()
                .setIfAbsent(generateKey(key), "lock");
    }

    public void unlock(Long key){
        redisTemplate.delete(generateKey(key));
    }

    public String generateKey(Long key){
        return key.toString();
    }
}
