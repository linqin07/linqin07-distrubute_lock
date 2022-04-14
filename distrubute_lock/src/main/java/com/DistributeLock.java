package com;

/**
 * @author linqin07
 * @title: DistributeLock
 * @description: TODO
 * @date 2022/4/1410:49 上午
 */
public interface DistributeLock {
    void lock() throws InterruptedException;

    void unlock();

    /**
     * 释放此分布式锁所需要的资源，比如zookeeper连接
     */
    void releaseResource();
}
