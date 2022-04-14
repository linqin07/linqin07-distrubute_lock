package com;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author linqin07
 * @title: NotReentrantDistributeLockWithHerdEffect
 * @description: 不可重入无羊群效应的分布式锁（公平锁）
 * @date 2022/4/1410:54 上午
 */
public class NotReentrantDistributeLock implements DistributeLock{
    private Thread currentThread;

    private String lockBasePath;
    private String lockPrefix;
    private String lockFullPath;

    private ZooKeeper zooKeeper;
    private String myId;
    private String myName;
    private int myTicket;

    public NotReentrantDistributeLock(String lockBasePath, String lockPrefix) throws IOException {
        this.currentThread = Thread.currentThread();
        this.lockBasePath = lockBasePath;
        this.lockPrefix = lockPrefix;
        this.lockFullPath = lockBasePath + "/" + lockPrefix;
        this.myId = UUID.randomUUID().toString();
        this.myName = Thread.currentThread().getName();
        // 这里直接连
        this.zooKeeper = ZookeeperUtil.getZookeeper();
        createLockBasePath();
    }

    private void createLockBasePath() {
        try {
            if (zooKeeper.exists(lockBasePath, null) == null) {
                zooKeeper.create(lockBasePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException ignored) {
            // ignored.printStackTrace();
        }
    }
    @Override
    public void lock() throws InterruptedException {
        System.out.println("begin get lock");
        try {
            String path = zooKeeper.create(lockFullPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            myTicket = extractMyTicket(path);
            String previousNode = buildPath(myTicket - 1);
            Stat exists = zooKeeper.exists(previousNode, event -> {
                synchronized (currentThread) {
                    currentThread.notify();
                    System.out.println("wake");
                }
            });
            if (exists != null) {
                synchronized (currentThread) {
                    currentThread.wait();
                }
            }
            System.out.println("get lock success");
        } catch (KeeperException e) {
            e.printStackTrace();
        }

    }

    private int extractMyTicket(String path) {
        int splitIndex = path.lastIndexOf("/");
        return Integer.parseInt(path.substring(splitIndex + 1).replace(lockPrefix, ""));
    }

    private String buildPath(int ticket) {
        return String.format("%s%010d", lockFullPath, ticket);
    }

    @Override
    public void unlock() {
        System.out.println("begin release lock");
        try {
            zooKeeper.delete(buildPath(myTicket), -1);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end release lock");
    }

    @Override
    public void releaseResource() {
        try {
            // 将zookeeper连接释放掉
            zooKeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
