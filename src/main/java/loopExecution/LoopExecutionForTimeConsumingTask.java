package loopExecution;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author lzn
 * @date 2023/07/14 21:54
 * @description If you are given an array and there will be duplicates in the array, and each number has to be SLEEP for the appropriate amount of time,
 * and then you can PUSH it into the list. For example, the value 2 from the list should sleep for 2 seconds.
 * And you need to ensure that all the numbers are sorted at the end as well.
 * <p>
 * 1. What would you do? Can you guarantee that the list will be sorted?
 * -- We can guarantee the list is sorted when we use single thread to execute, but the list will not be sorted if we want to more efficient and to speed up the process via multi-threads because of the overlapping execution
 * 2. What if this threads have 10000 or more elements?
 * --- multi-threads via ExecutorService
 */
@Slf4j
public class LoopExecutionForTimeConsumingTask {

    public static void main(String[] args) {
        // Simulate a array full of integer
        List<Integer> list = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            list.add(random.nextInt(10));
        }

        log.info("list before executed: {}", list);

        // Create a thread pool that have 20 core threads
        ExecutorService executorService = Executors.newFixedThreadPool(20);

        // Create a CountDownLatch to block the main thread and count down in the sub-threads
        CountDownLatch countDownLatch = new CountDownLatch(list.size());

        List<Integer> result = new ArrayList<>(list.size());

        log.info("Time start: " + new Date());

        for (int i = 0; i < list.size(); i++) {
            int curr = list.get(i);
            int finalI = i;
            executorService.submit(() -> {
                try {
                    log.info("{} - Thread: {}, curr: {}, Time before sleep: {}", finalI, Thread.currentThread().getName(), curr, new Date());

                    TimeUnit.SECONDS.sleep(curr);

                    log.info("{} - Thread: {}, curr: {}, Time after sleep: {}", finalI, Thread.currentThread().getName(), curr, new Date());
                    log.info("--------------------------------");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                result.add(curr);

                log.info("result: {}", result);

                countDownLatch.countDown();
            });
        }

        try {
            countDownLatch.await();

            log.info("current thread: {}", Thread.currentThread().getName());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("Time now: {}", new Date());

        executorService.shutdown();

        // Keep the list sorted
        Collections.sort(result);

        log.info("list after executed: {}", result);
    }
}
