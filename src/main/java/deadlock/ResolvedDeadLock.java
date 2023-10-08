package deadlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lzn
 * @date 2023/07/08 11:20
 * @description Resolve the deadlock problem via the tryLock method of ReentrantLock
 */
public class ResolvedDeadLock {

    private final Lock lock1 = new ReentrantLock();
    private final Lock lock2 = new ReentrantLock();

    private Thread method1() {

        return new Thread(() -> {
            boolean locked1 = false;
            boolean locked2 = false;
            try {
                locked1 = lock1.tryLock(1000, TimeUnit.MILLISECONDS);
                if (locked1) {
                    locked2 = lock2.tryLock(1000, TimeUnit.MILLISECONDS);
                    if (locked2) {
                        System.out.println("the method1 is running");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (locked1) {
                    lock1.unlock();
                }
                if (locked2) {
                    lock2.unlock();
                }
            }
        });
    }

    private Thread method2() {

        return new Thread(() -> {
            boolean locked1 = false;
            boolean locked2 = false;
            try {
                locked2 = lock1.tryLock(1000, TimeUnit.MILLISECONDS);
                if (locked2) {
                    locked1 = lock2.tryLock(1000, TimeUnit.MILLISECONDS);
                    if (locked1) {
                        System.out.println("the method2 is running");
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                if (locked1) {
                    lock1.unlock();
                }
                if (locked2) {
                    lock2.unlock();
                }
            }
        });
    }

    public static void main(String[] args) {
        ResolvedDeadLock deadLock = new ResolvedDeadLock();
        deadLock.method1().start();
        deadLock.method2().start();
    }
}
