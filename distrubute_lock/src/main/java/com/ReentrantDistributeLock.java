package com;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author linqin07
 * @title: ReentrantDistributeLock
 * @description: 可重入无羊群效应的分布式锁（公平锁）
 * @date 2022/4/1412:07 下午
 */
public class ReentrantDistributeLock implements DistributeLock {

    private final Thread currentThread;

    private String lockBasePath;
    private String lockPrefix;
    private String lockFullPath;

    private ZooKeeper zooKeeper;
    private String myName;
    private int myTicket;

    // 使用一个本机变量记载重复次数，这个值就不存储在zookeeper上了，
    // 一则修改zk节点值还需要网络会慢一些，二是其它节点只需要知道有个节点当前持有锁就可以了，至于重入几次不关它们的事呀
    private int reentrantCount = 0;

    public ReentrantDistributeLock(String lockBasePath, String lockName) throws IOException {
        this.lockBasePath = lockBasePath;
        this.lockPrefix = lockName;
        this.lockFullPath = lockBasePath + "/" + lockName;
        this.zooKeeper = ZookeeperUtil.getZookeeper();
        this.currentThread = Thread.currentThread();
        this.myName = currentThread.getName();
        createLockBasePath();
    }

    private void createLockBasePath() {
        try {
            if (zooKeeper.exists(lockBasePath, null) == null) {
                zooKeeper.create(lockBasePath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (KeeperException | InterruptedException ignored) {
        }
    }

    @Override
    public void lock() throws InterruptedException {
        log("begin get lock");

        // 如果reentrantCount不为0说明当前节点已经持有锁了，无需等待，直接增加重入次数即可
        if (reentrantCount != 0) {
            reentrantCount++;
            log("get lock success");
            return;
        }

        // 说明还没有获取到锁，需要设置watcher监听上一个节点释放锁事件
        try {
            String path = zooKeeper.create(lockFullPath, "".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            myTicket = extractMyTicket(path);
            String previousNode = buildPathByTicket(myTicket - 1);
            Stat exists = zooKeeper.exists(previousNode, event -> {
                synchronized (currentThread) {
                    currentThread.notify();
                    log("wake");
                }
            });
            if (exists != null) {
                synchronized (currentThread) {
                    currentThread.wait();
                }
            }
            reentrantCount++;
            log("get lock success");
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    private int extractMyTicket(String path) {
        int splitIndex = path.lastIndexOf("/");
        return Integer.parseInt(path.substring(splitIndex + 1).replace(lockPrefix, ""));
    }

    private String buildPathByTicket(int ticket) {
        return String.format("%s%010d", lockFullPath, ticket);
    }

    @Override
    public void unlock() {
        log("begin release lock");

        // 每次unlock的时候将递归次数减1，没有减到0说明还在递归中
        reentrantCount--;
        if (reentrantCount != 0) {
            log("end release lock");
            return;
        }

        // 只有当重入次数为0的时候才删除节点，将锁释放掉
        try {
            zooKeeper.delete(buildPathByTicket(myTicket), -1);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        log("end release lock");
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

    private void log(String msg) {
        System.out.printf("[%s], ticket=%d, reentrantCount=%d, threadName=%s, msg=%s\n", now(), myTicket, reentrantCount, myName, msg);
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}

