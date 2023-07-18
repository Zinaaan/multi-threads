package sequentialExecution;

import java.util.concurrent.*;

/**
 * @author lzn
 * @date 2023/07/08 21:40
 * @description
 */
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
            System.out.println(Thread.currentThread().getName() + " executed");
        })).thenRun(() -> executorService.execute(() -> {
            bankSimulatesByThreadJoin.withdraw(200);
            System.out.println(Thread.currentThread().getName() + " executed");
            countDownLatch.countDown();
        }));
        try {
            countDownLatch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("The current balance: " + bankSimulatesByThreadJoin.balance);
    }
}