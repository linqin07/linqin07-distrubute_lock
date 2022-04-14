import com.DistributeLock;
import com.NotReentrantDistributeLock;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author linqin07
 * @title: NotReentrantDistributeLockTest
 * @description: TODO
 * @date 2022/4/1412:03 下午
 */
public class NotReentrantDistributeLockTest {
    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static void main(String[] args) {

        int nodeNum = 10;
        for (int i = 0; i < nodeNum; i++) {
            new Thread(() -> {
                DistributeLock lock = null;
                try {
                    lock = new NotReentrantDistributeLock("/zk-lock", "bar");
                    lock.lock();
                    String myName = Thread.currentThread().getName();
                    System.out.println(myName + ": get lock success, now=" + now());
                    TimeUnit.SECONDS.sleep(3);
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
