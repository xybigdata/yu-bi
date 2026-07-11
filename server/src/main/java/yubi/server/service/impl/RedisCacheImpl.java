package yubi.server.service.impl;

import yubi.core.common.Cache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class RedisCacheImpl implements Cache {

    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisCacheImpl(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void put(String key, Object object) {
        redisTemplate.opsForValue().set(key, object);
    }

    @Override
    public void put(String key, Object object, int ttl) {
        redisTemplate.opsForValue().set(key, object, ttl, TimeUnit.SECONDS);
    }

    @Override
    public boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    @SuppressWarnings("unchecked") // Cache#get cannot retain T in the backing key-value store.
    public <T> T get(String key) {
        return (T) redisTemplate.opsForValue().get(key);
    }
}
