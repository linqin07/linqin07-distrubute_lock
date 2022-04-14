import com.DistributeLock;
import com.NotReentrantDistributeLockWithHerdEffect;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * @author linqin07
 * @title: NotReentrantDistributeLockWithHerdEffectTest
 * @description: TODO
 * @date 2022/4/1411:09 上午
 */
public class NotReentrantDistributeLockWithHerdEffectTest {
    private static String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static void main(String[] args) {
        int nodeNum = 10;
        for (int i = 0; i < nodeNum; i++) {
            new Thread(() -> {
                DistributeLock lock = null;
                try {
                    lock = new NotReentrantDistributeLockWithHerdEffect("/zk-lock", "foo");
                    lock.lock();
                    String myName = Thread.currentThread().getName();
                    System.out.println(myName + ": hold lock, now=" + now());
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
