package unsafeBuffer;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author lzn
 * @date 2023/10/08 14:54
 * @description Multi-threads operation of HashMap with UnsafeBuffer of Agrona library as key
 *
 * Key point:
 * 1. Instead of create a new key of map every time for aggregating and retrieving, use thread local to reuse it
 * 2. Don't forget to remove after using thread local object, especially in multithreaded env as it might cause memory leak
 */
@Slf4j
public class MultiThreadsForUnsafeBuffer {

    private static final MultiThreadsForUnsafeBuffer INSTANCE = new MultiThreadsForUnsafeBuffer();

    private MultiThreadsForUnsafeBuffer() {
    }

    public static MultiThreadsForUnsafeBuffer getInstance() {
        return INSTANCE;
    }

    private final Map<MetricBuffer, Long> latencyMap = new ConcurrentHashMap<>();
    private final ThreadLocal<MetricBuffer> reusableKeyBuffer = ThreadLocal.withInitial(() -> new MetricBuffer(ByteBuffer.allocate(16)));
    private final ThreadLocal<MetricBuffer> reusableRetrieveBuffer = ThreadLocal.withInitial(() -> new MetricBuffer(ByteBuffer.allocate(16)));

    public void aggregateMetrics(byte[] bytes) {
        try {
            MetricBuffer keyBuffer = reusableKeyBuffer.get();
            keyBuffer.wrap(bytes);
            log.info("contains: {},clientId: {}, point: {}, latency: {}",latencyMap.containsKey(keyBuffer), keyBuffer.getClientId(), keyBuffer.getPoint(), keyBuffer.getLatency());
            latencyMap.put(keyBuffer, keyBuffer.getLatency());
        } catch (Exception e) {
            log.error("Error in aggregateMetrics: {}", e.getMessage());
        } finally {
            reusableKeyBuffer.remove();
        }
    }

    public Long getLatency(byte[] bytes) {
        MetricBuffer keyBuffer = reusableRetrieveBuffer.get();
        Long latency = 0L;
        try {
            keyBuffer.wrap(bytes);
            log.info("get key clientId: {}, point: {}, latency: {}", keyBuffer.getClientId(), keyBuffer.getPoint(), keyBuffer.getLatency());
            latency = latencyMap.get(keyBuffer);
        } catch (Exception e) {
            log.error("Error in getLatency: {}", e.getMessage());
        } finally {
            reusableRetrieveBuffer.remove();
        }
        return latency;
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
            MetricBuffer unsafeBuffer = new MetricBuffer(ByteBuffer.allocate(16));
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
        log.info("Map size: {}", creationByThreadLocal.getLatencyMap().size());
        List<Future<Long>> latencyList = new ArrayList<>();
        for (int i = 0; i < populateCount; i++) {
            final int finalI = i;
            Future<Long> latencyFuture = executorService.submit(() -> creationByThreadLocal.getLatency(keyList.get(finalI)));
            latencyList.add(latencyFuture);
        }

        latencyList.forEach(longFuture -> {
            try {
                Long latency = longFuture.get();
                log.info("latency: {}", latency);
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
