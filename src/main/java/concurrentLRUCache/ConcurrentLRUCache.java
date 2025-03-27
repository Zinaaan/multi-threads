package concurrentLRUCache;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lzn
 * @date 2023/07/16 18:03
 * The implementation of multi-thread version for LRU cache
 * <p>
 * 1. Build a new Node class that includes key and value to indicates the element of LRU cache
 * 2. Build a bidirectional linked list class have both prev pointer, next pointer and Node element
 * 3. Build a LRU cache class with HashMap<key, Node> and BidirectionalLinkedList<Node>.
 * 4. Introducing a thread-safe container -> ConcurrentHashMap
 * 5. Introducing the ReentrantReadWriteLock for isolating the read and write events such as read-lock in read method and write-lock in write method
 * Rules: Put the frequent used data into the end of the bidirectionalLinkedList, and the LRU data into the beginning of the bidirectionalLinkedList
 * <p>
 * Functional:
 * 1. get: If key is not exists in the hashmap, return -1. Otherwise return Node.value and switch the value to the frequent used position
 * 2. put: If key is existing in the hashmap, change the old value to the new one and switch the value to the frequent used position
 * --Otherwise:
 * <1>. If the hashmap is full, evict the LRU data and put the current data into the beginning of the bidirectionalLinkedList
 * <2>. Otherwise just put the current data into the beginning of the bidirectionalLinkedList
 */
@Slf4j
public class ConcurrentLRUCache {

    private final ConcurrentHashMap<Integer, Node> cache;
    private final ReentrantReadWriteLock lock;
    private final BidirectionalLinkedList list;
    private final int capacity;

    public ConcurrentLRUCache(int capacity) {
        cache = new ConcurrentHashMap<>();
        list = new BidirectionalLinkedList();
        lock = new ReentrantReadWriteLock();
        this.capacity = capacity;
    }

    public Node get(int key) {
        Node curr = cache.get(key);
        if (curr != null) {
            lock.readLock().lock();
            try {

                // Invoke put method in multi-thread environment will cause deadlock as the put method will acquire write lock as well
                // Because a write lock is exclusive and does not allow concurrent read or write access.
                // Since you already have a read lock acquired, the writeLock().lock() call will block until all read locks are released.
//                put(key, curr.value);

                Node node = list.removeNode(curr);
                // If successfully removed the node
                if (node.prev == null && node.next == null) {
                    // put the value at the beginning of the bidirectional linked list
                    list.addFirst(curr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                lock.readLock().unlock();
            }
        }

        return curr;
    }

    public void put(int key, int value) {
        lock.writeLock().lock();
        Node newNode = new Node(key, value);
        try {
            if (cache.containsKey(key)) {
                list.removeNode(cache.get(key));
            } else {
                // If cache is full
                if (cache.size() == capacity) {
                    // Evict the LRU node
                    Node lruNode = list.removeLast();
                    cache.remove(lruNode.key);
                }
            }
            // put the data into the beginning
            cache.put(key, newNode);
            list.addFirst(newNode);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean evict(int key) {
        lock.writeLock().lock();
        try {
            if (cache.containsKey(key)) {
                Node remove = cache.remove(key);
                list.removeNode(remove);
                return true;
            }
        } catch (Exception e) {
            log.error("Error in removing key: {}, reason: {}", key, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
        return false;
    }

    @Override
    public String toString() {
        return "ConcurrentLRUCache{" +
                "cache=" + cache +
                ", list=" + list +
                ", capacity=" + capacity +
                '}';
    }

    private static class BidirectionalLinkedList {
        private final Node head;
        private final Node tail;

        public BidirectionalLinkedList() {
            head = new Node(0, 0);
            tail = new Node(0, 0);

            // Build the bidirectional linked list
            head.next = tail;
            tail.prev = head;
        }

        public void addFirst(Node node) {
            node.next = head.next;
            node.prev = head;
            head.next.prev = node;
            head.next = node;
        }

        public Node removeNode(Node node) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            return node;
        }

        public Node removeLast() {
            if (head.next == tail) {
                return head;
            }
            return removeNode(tail.prev);
        }

        @Override
        public String toString() {
            return "BidirectionalLinkedList{" +
                    "head=" + head +
                    ", tail=" + tail +
                    '}';
        }
    }

    private static class Node {
        private final int key;
        private final int value;
        private Node prev;
        private Node next;

        public Node(int key, int value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int capacity = 3;
        ConcurrentLRUCache cache = new ConcurrentLRUCache(capacity);

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
            log.info("cache.get(4) == null? {}", cache.get(4) == null);
            // 10
            log.info("cache.get(1).value: {}", cache.get(1).value);
            // 20
            log.info("cache.get(2).value: {}", cache.get(2).value);
            latch.countDown();
        });

        // Thread 3: Remove a key from the cache
        executorService.submit(() -> {
            cache.evict(3);
            // True
            log.info("cache.get(3) == null? {}", cache.get(3) == null);
            latch.countDown();
        });

        // Thread 4: Put values exceeding the capacity to trigger eviction
        executorService.submit(() -> {
            cache.put(4, 40);
            // False
            log.info("cache.get(1) == null? {}", cache.get(1) == null);
            latch.countDown();
        });

        // Wait for all threads to complete
        latch.await();
        executorService.shutdown();
        // Verify cache state after all operations
        // True, Key 3 should have been removed
        log.info("cache.get(3) == null? {}", cache.get(3) == null);
        // 10, Key 1 should still be in the cache
        log.info("cache.get(1).value: {}", cache.get(1).value);
        // 20,  Key 2 should still be in the cache
        log.info("cache.get(2).value: {}", cache.get(2).value);
        // False, Key 4 should be in the cache
        log.info("cache.get(4) == null? {}", cache.get(4) == null);
    }
}
