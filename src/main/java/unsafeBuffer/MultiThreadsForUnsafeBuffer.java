package unsafeBuffer;

import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author lzn
 * @date 2023/10/08 14:54
 * @description Multi-threads operation of HashMap with UnsafeBuffer of Agrona library as key
 */
public class MultiThreadsForUnsafeBuffer {

    private static final MultiThreadsForUnsafeBuffer INSTANCE = new MultiThreadsForUnsafeBuffer();

    private MultiThreadsForUnsafeBuffer() {
    }

    public static MultiThreadsForUnsafeBuffer getInstance() {
        return INSTANCE;
    }

    private final Map<UnsafeBuffer, Long> latencyMap = new HashMap<>();
    private final ThreadLocal<UnsafeBuffer> reusableKeyBuffer = ThreadLocal.withInitial(() -> new UnsafeBuffer(ByteBuffer.allocate(8)));
    private final ThreadLocal<UnsafeBuffer> reusableRetrieveBuffer = ThreadLocal.withInitial(() -> new UnsafeBuffer(ByteBuffer.allocate(8)));
    private final ReentrantLock lock = new ReentrantLock();

    public void populateMetrics(UnsafeBuffer unsafeBuffer) {
        int clientId = unsafeBuffer.getInt(0);
        int point = unsafeBuffer.getInt(4);
        long latency = unsafeBuffer.getLong(8);
        UnsafeBuffer keyBuffer = reusableKeyBuffer.get();
        keyBuffer.putInt(0, clientId);
        keyBuffer.putInt(4, point);
        lock.lock();
        try {
            if (!latencyMap.containsKey(keyBuffer)) {
                UnsafeBuffer key = new UnsafeBuffer(ByteBuffer.allocate(8));
                key.putInt(0, clientId);
                key.putInt(4, point);
                System.out.println("new key clientId: " + clientId + ", point: " + point);
                latencyMap.put(key, latency);
            } else {
                System.out.println("old key clientId: " + clientId + ", point: " + point);
                latencyMap.put(keyBuffer, latency);
            }
        } finally {
            lock.unlock();
        }
    }

    public long getLatency(UnsafeBuffer unsafeBuffer) {
        int clientId = unsafeBuffer.getInt(0);
        int point = unsafeBuffer.getInt(4);
        UnsafeBuffer keyBuffer = reusableRetrieveBuffer.get();
        keyBuffer.putInt(0, clientId);
        keyBuffer.putInt(4, point);
        System.out.println("get key clientId: " + clientId + ", point: " + point + ", latency: " + unsafeBuffer.getLong(8));
        return latencyMap.getOrDefault(keyBuffer, 0L);
    }

    public Map<UnsafeBuffer, Long> getLatencyMap() {
        return latencyMap;
    }

    public static void main(String[] args) throws InterruptedException {
        MultiThreadsForUnsafeBuffer creationByThreadLocal = MultiThreadsForUnsafeBuffer.getInstance();
        Random random = new Random();
        int threadCount = 10;
        int populateCount = 50;
        CountDownLatch latch = new CountDownLatch(populateCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        LocalDateTime startTime = LocalDateTime.now();

        List<UnsafeBuffer> keyList = new ArrayList<>();
        for (int i = 0; i < populateCount; i++) {
            long start = System.nanoTime();
            UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(16));
            unsafeBuffer.putInt(0, random.nextInt(100) + 5);
            unsafeBuffer.putInt(4, random.nextInt(5));
//            unsafeBuffer.putInt(0, 5);
//            unsafeBuffer.putInt(4, 3);
            unsafeBuffer.putLong(8, System.nanoTime() - start);
            keyList.add(unsafeBuffer);
        }
        for (int i = 0; i < populateCount; i++) {
            final int finalI = i;
            executorService.execute(() -> {
                creationByThreadLocal.populateMetrics(keyList.get(finalI));
                latch.countDown();
            });
        }

        latch.await();
        System.out.println("Map: " + creationByThreadLocal.getLatencyMap());
        List<Future<Long>> latencyList = new ArrayList<>();
        for (int i = 0; i < populateCount; i++) {
            final int finalI = i;
            Future<Long> latencyFuture = executorService.submit(() -> creationByThreadLocal.getLatency(keyList.get(finalI)));
            latencyList.add(latencyFuture);
        }

        latencyList.forEach(longFuture -> {
            try {
                System.out.println("latency: " + longFuture.get());
            } catch (InterruptedException e) {
                System.out.println("Error on interrupted: " + e.getMessage());
            } catch (ExecutionException e) {
                System.out.println("Error on Execution: " + e.getMessage());
            }
        });

        executorService.shutdown();
        System.out.println("Time cost: " + Duration.between(startTime, LocalDateTime.now()).getNano() / 1000 / 1000 + " milliseconds");
    }
}
