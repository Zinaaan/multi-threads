package producerConsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author lzn
 * @date 2023/07/19 21:28
 * @description
 */
public class ProducerConsumerByThread {
    private final int capacity = 10;
    private final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(capacity);
    private final Object object = new Object();

    private Thread producer() {
        return new Thread(() -> {
            while (true) {
                synchronized (object) {
                    while (queue.size() == capacity) {
                        System.out.println("Buffer is full. Producer is waiting...");
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String message = "message";
                    queue.offer(message);
                    System.out.println("write message: " + message);
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
                        System.out.println("Buffer is empty. Consumer is waiting...");
                        try {
                            object.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String message = queue.poll();
                    System.out.println("read message: " + message);
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
