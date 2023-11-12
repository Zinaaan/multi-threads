package producerConsumer;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @author lzn
 * @date 2023/07/19 21:28
 * @description
 */
@Slf4j
public class ProducerConsumerBySemaphore {

    private final int capacity = 10;
    private final Queue<String> queue = new ArrayDeque<>(capacity);
    private final Semaphore semaphore = new Semaphore(0);

    private Thread producer() {
        return new Thread(() -> {
            while (true) {
                String message = "message";
                queue.offer(message);
                semaphore.release();
                log.info("write message: {}", message);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Thread consumer() {
        return new Thread(() -> {
            while (true) {
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(!queue.isEmpty()){
                    String message = queue.poll();
                    log.info("read message: {}", message);
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public static void main(String[] args) {
        ProducerConsumerBySemaphore producerConsumerByThread = new ProducerConsumerBySemaphore();
        producerConsumerByThread.producer().start();
        producerConsumerByThread.consumer().start();
    }
}
