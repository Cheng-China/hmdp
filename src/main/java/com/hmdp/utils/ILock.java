package com.hmdp.utils;

public interface ILock {

    /**
     * 尝试获取锁
     * @param timeout 超时自动释放时间
     * @return true 获取锁成功，false 获取失败
     */
    boolean tryLock(long timeout);

    void unlock();
}
