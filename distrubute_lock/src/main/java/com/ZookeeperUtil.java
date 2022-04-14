package com;

import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author linqin07
 * @title: ZookeeperUtil
 * @description: TODO
 * @date 2022/4/1411:01 上午
 */
public class ZookeeperUtil {
    private ZooKeeper zooKeeper;

    public static ZooKeeper getZookeeper() throws IOException {
        return new ZooKeeper("127.0.0.1:2181", 5000, null);
    }

    public static void main(String[] args) {
        ReentrantLock reentrantLock = new ReentrantLock();
    }
}
