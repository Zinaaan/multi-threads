package concurrentLRUCache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author lzn
 * @date 2023/07/16 18:03
 * @description The implementation of multi-thread version for LRU cache
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
 * <p>
 * 2. put: If key is exists in the hashmap, change the old value to the new one and switch the value to the frequent used position
 * --Otherwise:
 * <1>. If the hashmap is full, evict the LRU data and put the current data into the beginning of the bidirectionalLinkedList
 * <2>. Otherwise just put the current data into the beginning of the bidirectionalLinkedList
 */
public class ConcurrentLruCache {

    private final ConcurrentHashMap<Integer, Node> cache;
    private final ReentrantReadWriteLock lock;
    private final BidirectionalLinkedList list;
    private final int capacity;

    public ConcurrentLruCache(int capacity) {
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

                // Can not invoke put method, it will case deadlock as the put method will acquire write lock as well
//                put(key, curr.value);

                list.removeNode(cache.get(key));
                // put the data into the beginning
                list.addFirst(curr);
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

    @Override
    public String toString() {
        return "ConcurrentLruCache{" +
                "cache=" + cache +
                ", list=" + list +
                ", capacity=" + capacity +
                '}';
    }

    public BidirectionalLinkedList getList() {
        return list;
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

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch countDownLatch = new CountDownLatch(10);
        ConcurrentHashMap<String, List<String>> result = new ConcurrentHashMap<>();

        for (int i = 0; i < 10; i++) {
            executorService.execute(() -> {
                ConcurrentLruCache concurrentLruCache = new ConcurrentLruCache(3);
                concurrentLruCache.put(1, 5);
                concurrentLruCache.put(2, 3);
                concurrentLruCache.put(5, 3);
                concurrentLruCache.put(4, 1);
                concurrentLruCache.get(5);
                countDownLatch.countDown();

                BidirectionalLinkedList linkedList = concurrentLruCache.getList();
                Node dummy = linkedList.head.next;
                while (dummy != null) {
                    result.putIfAbsent(Thread.currentThread().getName(), new ArrayList<>());
                    result.get(Thread.currentThread().getName()).add("key: " + dummy.key + ", value: " + dummy.value);
                    dummy = dummy.next;
                }
            });
        }

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("result: " + result);
        executorService.shutdown();
    }
}
