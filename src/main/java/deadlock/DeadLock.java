package deadlock;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lzn
 * @date 2023/07/08 11:20
 * @description Typical example for deadlock
 */
@Slf4j
public class DeadLock {

    private final String str1 = "2";
    private final String str2 = "3";

    private Thread method1() {
        return new Thread(() -> {
            synchronized (str1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (str2) {
                    log.info("the method1 is running");
                }
            }
        });
    }

    private Thread method2() {
        return new Thread(() -> {
            synchronized (str2) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (str1) {
                    log.info("the method2 is running");
                }
            }
        });
    }

    public static void main(String[] args) {
        DeadLock deadLock = new DeadLock();
        deadLock.method1().start();
        deadLock.method2().start();
    }
}
