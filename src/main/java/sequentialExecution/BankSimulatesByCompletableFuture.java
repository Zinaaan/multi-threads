package sequentialExecution;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * @author lzn
 * @date 2023/07/08 21:40
 * @description
 */
@Slf4j
public class BankSimulatesByCompletableFuture {

    private int balance;

    public BankSimulatesByCompletableFuture() {
        this.balance = 0;
    }

    public void deposit(int number) {
        balance += number;
    }

    public void withdraw(int number) {
        if (balance == 0) {
            throw new IllegalArgumentException("The current balance is 0");
        }

        balance -= number;
    }

    public static void main(String[] args) {
        BankSimulatesByCompletableFuture bankSimulatesByThreadJoin = new BankSimulatesByCompletableFuture();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        CompletableFuture.runAsync(() -> executorService.execute(() -> {
            bankSimulatesByThreadJoin.deposit(500);
            log.info("{} executed", Thread.currentThread().getName());
        })).thenRun(() -> executorService.execute(() -> {
            bankSimulatesByThreadJoin.withdraw(200);
            log.info("{} executed", Thread.currentThread().getName());
            countDownLatch.countDown();
        }));
        try {
            countDownLatch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("The current balance: {}", bankSimulatesByThreadJoin.balance);
    }
}