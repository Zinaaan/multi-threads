package unsafeBuffer;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MultiThreadsForUnsafeBuffer {

    private static final MultiThreadsForUnsafeBuffer INSTANCE = new MultiThreadsForUnsafeBuffer();

    private MultiThreadsForUnsafeBuffer() {
    }

    public static MultiThreadsForUnsafeBuffer getInstance() {
        return INSTANCE;
    }

    private final Map<MetricBuffer, Long> latencyMap = new HashMap<>();
    private final ThreadLocal<MetricBuffer> reusableKeyBuffer = ThreadLocal.withInitial(() -> new MetricBuffer(ByteBuffer.allocate(8)));
    private final ThreadLocal<MetricBuffer> reusableRetrieveBuffer = ThreadLocal.withInitial(() -> new MetricBuffer(ByteBuffer.allocate(8)));
    private final ReentrantLock lock = new ReentrantLock();

    public void aggregateMetrics(byte[] bytes) {
        MetricBuffer keyBuffer = reusableKeyBuffer.get();
        keyBuffer.wrap(bytes);
        lock.lock();
        try {
            if (!latencyMap.containsKey(keyBuffer)) {
                MetricBuffer key = new MetricBuffer(ByteBuffer.allocate(8));
                key.wrap(bytes);
                log.info("new key clientId: {}, point: {}, latency: {}", key.getClientId(), key.getPoint(), key.getLatency());
                latencyMap.put(key, keyBuffer.getLatency());
            } else {
                log.info("old key clientId: {}, point: {}, latency: {}", keyBuffer.getClientId(), keyBuffer.getPoint(), keyBuffer.getLatency());
                latencyMap.put(keyBuffer, keyBuffer.getLatency());
            }
        } finally {
            lock.unlock();
        }
    }

    public long getLatency(byte[] bytes) {
        MetricBuffer keyBuffer = reusableRetrieveBuffer.get();
        keyBuffer.wrap(bytes);
        log.info("get key clientId: {}, point: {}, latency: {}", keyBuffer.getClientId(), keyBuffer.getPoint(), keyBuffer.getLatency());
        return latencyMap.getOrDefault(keyBuffer, 0L);
    }

    public Map<MetricBuffer, Long> getLatencyMap() {
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

        List<byte[]> keyList = new ArrayList<>();
        for (int i = 0; i < populateCount; i++) {
            long start = System.nanoTime();
            UnsafeBuffer unsafeBuffer = new UnsafeBuffer(ByteBuffer.allocate(16));
            unsafeBuffer.putInt(0, random.nextInt(100) + 5);
            unsafeBuffer.putInt(4, random.nextInt(5));
//            unsafeBuffer.putInt(0, 5);
//            unsafeBuffer.putInt(4, 3);
            unsafeBuffer.putLong(8, System.nanoTime() - start);
            keyList.add(unsafeBuffer.byteArray());
        }
        for (int i = 0; i < populateCount; i++) {
            final int finalI = i;
            executorService.execute(() -> {
                creationByThreadLocal.aggregateMetrics(keyList.get(finalI));
                latch.countDown();
            });
        }

        latch.await();
        log.info("Map: {}", creationByThreadLocal.getLatencyMap());
        List<Future<Long>> latencyList = new ArrayList<>();
        for (int i = 0; i < populateCount; i++) {
            final int finalI = i;
            Future<Long> latencyFuture = executorService.submit(() -> creationByThreadLocal.getLatency(keyList.get(finalI)));
            latencyList.add(latencyFuture);
        }

        latencyList.forEach(longFuture -> {
            try {
                log.info("latency: {}", longFuture.get());
            } catch (InterruptedException e) {
                log.info("Error on interrupted: {}", e.getMessage());
            } catch (ExecutionException e) {
                log.info("Error on Execution: {}", e.getMessage());
            }
        });

        executorService.shutdown();
        log.info("Time cost: {} milliseconds", Duration.between(startTime, LocalDateTime.now()).getNano() / 1000 / 1000);
    }
}
