package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 利用 redis 的 setnx 实现互斥锁
 */
public class Lock {
    public static boolean tryLock(StringRedisTemplate stringRedisTemplate, String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    public static void unlock(StringRedisTemplate stringRedisTemplate, String key){
        stringRedisTemplate.delete(key);
    }
}
