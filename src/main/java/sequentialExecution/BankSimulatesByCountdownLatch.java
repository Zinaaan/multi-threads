package sequentialExecution;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;

/**
 * @author lzn
 * @date 2023/07/08 21:40
 * @description
 */
@Slf4j
public class BankSimulatesByCountdownLatch {

    private int balance;

    public BankSimulatesByCountdownLatch() {
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
        BankSimulatesByCountdownLatch bankSimulatesByCountdownLatch = new BankSimulatesByCountdownLatch();
        // CountDownLatch1 for stopping main thread and waiting for the child thread execution completed
        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        // CountDownLatch2 for guaranteeing the depositThread executed before the withDrawThread(sequential thread execution)
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        Thread depositThread = new Thread(() -> {
            log.info("{} executed", Thread.currentThread().getName());
            bankSimulatesByCountdownLatch.deposit(500);
            countDownLatch2.countDown();
        }, "depositThread");

        Thread withDrawThread = new Thread(() -> {
            try {
                countDownLatch2.await();
                log.info("{} executed", Thread.currentThread().getName());
                bankSimulatesByCountdownLatch.withdraw(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                countDownLatch1.countDown();
            }
        }, "withDrawThread");

        depositThread.start();
        withDrawThread.start();
        try {
            countDownLatch1.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log.info("The current balance: {}", bankSimulatesByCountdownLatch.balance);
    }
}