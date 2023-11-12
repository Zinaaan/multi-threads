package alternateExecution;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lzn
 * @date 2023/07/03 20:38
 * @description Tips: After invoking the notify(), the current thread will not release the lock and wake up other thread immediately unless the synchronized block is completed
 * <p>
 * 1. Used isContinue to guarantee that "a" will output firstly
 * 2. Guarantee alternate execution by lock.wait() and lock.notify() -> (stop executing the thread1 and release lock, so the thread2 will acquire this lock and start executing)
 * 3. Used AtomicInteger to accumulate the execution times
 * 4. Make sure that the child thread will exit the entire process via "if (atomicInteger.get() > 100) return;" so that to signal the main to continue processing
 */
@Slf4j
public class AlternateExecutionByLock {

    static boolean isContinue = true;

    public static void main(String[] args) {
        Object lock = new Object();
        AtomicInteger atomicInteger = new AtomicInteger(1);
        Thread thread1 = new Thread(() -> {
            synchronized (lock) {
                while (isContinue) {
                    log.info("{}a", atomicInteger.get());
                    lock.notify();
                    try {
                        atomicInteger.incrementAndGet();
                        // The current value must be checked first
                        if (atomicInteger.get() > 100) {
                            return;
                        }
                        isContinue = false;
                        // Block current thread and release lock
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "thread1");

        Thread thread2 = new Thread(() -> {
            synchronized (lock) {
                while (!isContinue) {
                    log.info("{}b", atomicInteger.get());
                    lock.notify();
                    try {
                        atomicInteger.incrementAndGet();
                        // The current value must be checked first
                        if (atomicInteger.get() > 100) {
                            return;
                        }
                        isContinue = true;
                        // Block current thread and release lock
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, "thread2");

        thread1.start();
        thread2.start();
    }
}
