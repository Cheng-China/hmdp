package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;


import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private StringRedisTemplate stringRedisTemplate;

    private String name;

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true);

    //类加载时初始化释放锁的 lua 脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }

    @Override
    public boolean tryLock(long timeout) {
        //获取线程标识
        String threadId = ID_PREFIX + "-" +Thread.currentThread().getId();
        //利用 setnx 机制实现获取锁
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId , timeout, TimeUnit.SECONDS);
        //直接返回会自动拆箱，会有空指针异常
//        return flag
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void unlock() {
        String threadId = ID_PREFIX + "-" +Thread.currentThread().getId();
        String redisValue = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
       /* if (threadId.equals(redisValue)){
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }*/

        //调用 lua 脚本原子的执行释放锁操作
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(threadId), redisValue);

    }
}
