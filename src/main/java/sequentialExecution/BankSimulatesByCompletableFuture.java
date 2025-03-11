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
        CountDownLatch cdForWithdraw = new CountDownLatch(1);
        CountDownLatch cdForMain = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            bankSimulatesByThreadJoin.deposit(500);
            log.info("Deposited: {}", 500);
            cdForWithdraw.countDown();
        });
        CompletableFuture.runAsync(() -> {
            try {
                cdForWithdraw.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            bankSimulatesByThreadJoin.withdraw(300);
            log.info("Withdrew {}", 300);
            cdForMain.countDown();
        });
        try {
            cdForMain.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("The current balance: {}", bankSimulatesByThreadJoin.balance);
    }
}