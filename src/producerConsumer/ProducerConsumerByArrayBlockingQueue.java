package producerConsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lzn
 * @date 2023/07/19 21:28
 * @description
 */
public class ProducerConsumerByArrayBlockingQueue {
    private final int capacity = 10;
    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(capacity);

    private Thread producer() {
        return new Thread(() -> {
            while (true) {
                String message = "message";
                try {
                    // Write a message, waiting here if the queue is full
                    queue.put(message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("write message: " + message);
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
                String message = null;
                try {
                    // Got a message, waiting here if queue is empty
                    message = queue.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("read message: " + message);
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) {
        ProducerConsumerByArrayBlockingQueue producerConsumerByThread = new ProducerConsumerByArrayBlockingQueue();
        producerConsumerByThread.producer().start();
        producerConsumerByThread.consumer().start();
    }
}
