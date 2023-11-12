package producerConsumer;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lzn
 * @date 2023/07/19 21:28
 * @description
 */
@Slf4j
public class ProducerConsumerByThread {
    private final int capacity = 10;
    private final Queue<String> queue = new ArrayDeque<>(capacity);
    private final Object object = new Object();

    private Thread producer() {
        return new Thread(() -> {
            while (true) {
                synchronized (object) {
                    while (queue.size() == capacity) {
                        log.info("Buffer is full. Producer is waiting...");
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String message = "message";
                    queue.offer(message);
                    log.info("write message: {}", message);
                    object.notify();
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private Thread consumer() {
        return new Thread(() -> {
            while (true) {
                synchronized (object) {
                    while (queue.isEmpty()) {
                        log.info("Buffer is empty. Consumer is waiting...");
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String message = queue.poll();
                    log.info("read message: {}", message);
                    object.notify();
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
        ProducerConsumerByThread producerConsumerByThread = new ProducerConsumerByThread();
        producerConsumerByThread.producer().start();
        producerConsumerByThread.consumer().start();
    }
}
