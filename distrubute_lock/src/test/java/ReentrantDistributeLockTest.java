/**
 * @author linqin07
 * @title: ReentrantDistributeLockTest
 * @description: TODO
 * @date 2022/4/1412:10 下午
 */

import com.DistributeLock;
import com.ReentrantDistributeLock;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 测试分布式可重入锁，每个锁随机重入，看是否会发生错误
 *
 * @author CC11001100
 */
public class ReentrantDistributeLockTest {

    public static void main(String[] args) {

        int nodeNum = 10;
        for (int i = 0; i < nodeNum; i++) {
            new Thread(() -> {
                DistributeLock lock = null;
                try {
                    lock = new ReentrantDistributeLock("/zk-lock", "bar");
                    lock.lock();
                    TimeUnit.SECONDS.sleep(1);

                    int reentrantTimes = new Random().nextInt(10);
                    int reentrantCount = 0;
                    for (int j = 0; j < reentrantTimes; j++) {
                        lock.lock();
                        reentrantCount++;
                        if (Math.random() < 0.5) {
                            lock.unlock();
                            reentrantCount--;
                        }
                    }
                    while (reentrantCount-- > 0) {
                        lock.unlock();
                    }

                    lock.unlock();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    if (lock != null) {
                        lock.releaseResource();
                    }
                }
            }, "thread-name-" + i).start();
        }
    }
}