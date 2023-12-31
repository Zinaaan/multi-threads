package alternateExecution;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author lzn
 * @date 2023/07/03 22:14
 * @description
 */
@Slf4j
public class AlternateExecutionByCompletableFuture {
    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger();
    private static final int MAX_NUM = 100;
    private static final Semaphore SEMAPHORE_A = new Semaphore(1);
    private static final Semaphore SEMAPHORE_B = new Semaphore(0);

    public static void main(String[] args) {
        CompletableFuture.runAsync(() -> {
            while (ATOMIC_INTEGER.get() < MAX_NUM) {
                try {
                    SEMAPHORE_A.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ATOMIC_INTEGER.incrementAndGet();
                if (ATOMIC_INTEGER.get() > MAX_NUM) {
                    return;
                }
                log.info("{}a", ATOMIC_INTEGER.get());
                SEMAPHORE_B.release();
            }
        });

        CompletableFuture.runAsync(() -> {
            while (ATOMIC_INTEGER.get() < MAX_NUM) {
                try {
                    SEMAPHORE_B.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                ATOMIC_INTEGER.incrementAndGet();
                if (ATOMIC_INTEGER.get() > MAX_NUM) {
                    return;
                }
                log.info("{}b", ATOMIC_INTEGER.get());
                SEMAPHORE_A.release();
            }
        }).join();

    }
}
