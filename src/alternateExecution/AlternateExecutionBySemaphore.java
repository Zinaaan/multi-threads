package alternateExecution;

import java.util.concurrent.Semaphore;

/**
 * @author lzn
 * @date 2023/07/03 21:40
 * @description
 */
public class AlternateExecutionBySemaphore {

    static Semaphore semaphoreA = new Semaphore(1);
    static Semaphore semaphoreB = new Semaphore(0);

    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                try {
                    semaphoreA.acquire();
                    System.out.println("a");
                    semaphoreB.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                try {
                    semaphoreB.acquire();
                    System.out.println("b");
                    semaphoreA.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        thread1.start();
        thread2.start();
    }
}
