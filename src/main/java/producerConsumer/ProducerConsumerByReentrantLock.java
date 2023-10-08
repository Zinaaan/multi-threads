package producerConsumer;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lzn
 * @date 2023/07/19 21:28
 * @description
 */
public class ProducerConsumerByReentrantLock {
    private final int capacity = 10;
    private final Queue<String> queue = new ArrayDeque<>(capacity);
    private final Lock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();

    private Thread producer() {
        return new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (queue.size() == capacity) {
                        System.out.println("Buffer is full. Producer is waiting...");
                        try {
                            notFull.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String message = "message";
                    queue.offer(message);
                    System.out.println("write message: " + message);
                    notEmpty.signal();
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }

            }
        });
    }

    private Thread consumer() {
        return new Thread(() -> {
            while (true) {
                lock.lock();
                try {
                    while (queue.isEmpty()) {
                        System.out.println("Buffer is empty. Consumer is waiting...");
                        try {
                            notEmpty.await();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    String message = queue.poll();
                    System.out.println("read message: " + message);
                    notFull.signal();

                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public static void main(String[] args) {
        ProducerConsumerByReentrantLock producerConsumerByThread = new ProducerConsumerByReentrantLock();
        producerConsumerByThread.producer().start();
        producerConsumerByThread.consumer().start();
    }
}
