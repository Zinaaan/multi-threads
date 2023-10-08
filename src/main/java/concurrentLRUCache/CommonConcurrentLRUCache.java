package concurrentLRUCache;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lzn
 * @date 2023/07/16 18:03
 * @description The implementation of multi-thread version for common LRU cache
 * Optimization:
 * Introducing a ReentrantReadWriteLock to separate read and write operations, which is useful in the current situation where there are more reads than writes.
 */
public class CommonConcurrentLRUCache<K, V> {

    private final ConcurrentHashMap<K, V> cache;
    private final ConcurrentLinkedDeque<K> queue;
    private final ReentrantReadWriteLock lock;
    private final int capacity;

    public CommonConcurrentLRUCache(int capacity) {
        cache = new ConcurrentHashMap<>();
        queue = new ConcurrentLinkedDeque<>();
        lock = new ReentrantReadWriteLock();
        this.capacity = capacity;
    }

    public V get(K key) {
        V curr = cache.get(key);
        if (curr != null) {
            lock.readLock().lock();
            try {
                if (queue.remove(key)) {
                    queue.offer(key);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.readLock().unlock();
            }
        }

        return curr;
    }

    public void put(K key, V value) {
        lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                queue.remove(key);
            } else {
                // If cache is full
                if (queue.size() == capacity) {
                    // Evict the LRU node
                    K lruNode = queue.removeLast();
                    cache.remove(lruNode);
                }
            }
            // put the data into the beginning
            cache.put(key, value);
            queue.offer(key);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean evict(K key) {
        lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                cache.remove(key);
                queue.remove(key);
                return true;
            }
        } catch (Exception e) {
            System.out.println("Error in removing key: " + key + ", reason: " + e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public String toString() {
        return "ConcurrentLRUCache{" +
                "cache=" + cache +
                ", capacity=" + capacity +
                '}';
    }

    public static void main(String[] args) throws InterruptedException {
        int capacity = 3;
        CommonConcurrentLRUCache<Integer, Integer> cache = new CommonConcurrentLRUCache<>(capacity);

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // Use CountDownLatch to wait for all threads to complete
        CountDownLatch latch = new CountDownLatch(4);

        // Thread 1: Put values into the cache
        executorService.submit(() -> {
            cache.put(1, 10);
            cache.put(2, 20);
            cache.put(3, 30);
            latch.countDown();
        });

        // Thread 2: Read values from the cache
        executorService.submit(() -> {
            // True
            System.out.println("cache.get(4) == null? " + (cache.get(4) == null));
            // 10
            System.out.println("cache.get(1).value: " + cache.get(1));
            // 20
            System.out.println("cache.get(2).value: " + cache.get(2));
            latch.countDown();
        });

        // Thread 3: Remove a key from the cache
        executorService.submit(() -> {
            cache.evict(3);
            // True
            System.out.println("cache.get(3) == null? " + (cache.get(3) == null));
            latch.countDown();
        });

        // Thread 4: Put values exceeding the capacity to trigger eviction
        executorService.submit(() -> {
            cache.put(4, 40);
            // False
            System.out.println("cache.get(1) == null? " + (cache.get(1) == null));
            latch.countDown();
        });

        // Wait for all threads to complete
        latch.await();
        executorService.shutdown();
        // Verify cache state after all operations
        // True, Key 3 should have been removed
        System.out.println("cache.get(3) == null? " + (cache.get(3) == null));
        // 10, Key 1 should still be in the cache
        System.out.println("cache.get(1).value: " + cache.get(1));
        // 20,  Key 2 should still be in the cache
        System.out.println("cache.get(2).value: " + cache.get(2));
        // False, Key 4 should be in the cache
        System.out.println("cache.get(4) == null? " + (cache.get(4) == null));
    }
}
