package com;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @author linqin07
 * @title: NotReentrantDistributeLockWithHerdEffect
 * @description: 不可重入有羊群效应的分布式锁（不公平锁）
 * @date 2022/4/1410:54 上午
 */
public class NotReentrantDistributeLockWithHerdEffect implements DistributeLock{
    private Thread currentThread;

    private String lockBasePath;
    private String lockName;
    private String lockFullPath;

    private ZooKeeper zooKeeper;
    private String myId;
    private String myName;

    public NotReentrantDistributeLockWithHerdEffect(String lockBasePath, String lockName) throws IOException {
        this.currentThread = Thread.currentThread();
        this.lockBasePath = lockBasePath;
        this.lockName = lockName;
        this.lockFullPath = lockBasePath + "/" + lockName;
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
        try {
            zooKeeper.create(lockFullPath, myId.getBytes(StandardCharsets.UTF_8), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            System.out.println(myName + ": get lock");
        } catch (KeeperException.NodeExistsException e) {
            // 如果节点已经存在，则监听此节点，当节点被删除时再抢占锁
            try {
                zooKeeper.exists(lockFullPath, event -> {
                    synchronized (currentThread) {
                        currentThread.notify();
                        System.out.println(myName + ": wake");
                    }
                });
            } catch (KeeperException.NoNodeException e1) {
                // 间不容发之际，其它人释放了锁
                lock();
            } catch (KeeperException | InterruptedException e1) {
                e1.printStackTrace();
            }
            synchronized (currentThread) {
                currentThread.wait();
            }
            // 唤醒后都跑这里去抢lock
            lock();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void unlock() {
        try {
            byte[] nodeBytes = zooKeeper.getData(lockFullPath, false, null);
            String currentHoldLockNodeId = new String(nodeBytes, StandardCharsets.UTF_8);
            // 只有当前锁的持有者是自己的时候，才能删除节点
            if (myId.equalsIgnoreCase(currentHoldLockNodeId)) {
                zooKeeper.delete(lockFullPath, -1);
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(myName + ": releaseResource lock");
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
