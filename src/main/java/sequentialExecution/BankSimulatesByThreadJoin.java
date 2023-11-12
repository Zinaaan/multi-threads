package sequentialExecution;

import lombok.extern.slf4j.Slf4j;

/**
 * @author lzn
 * @date 2023/07/08 21:40
 * @description
 */
@Slf4j
public class BankSimulatesByThreadJoin {

    private int balance;

    public BankSimulatesByThreadJoin() {
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
        BankSimulatesByThreadJoin bankSimulatesByThreadJoin = new BankSimulatesByThreadJoin();
        Thread depositThread = new Thread(() -> {
            bankSimulatesByThreadJoin.deposit(500);
            log.info("{} executed", Thread.currentThread().getName());
        }, "depositThread");
        Thread withDrawThread = new Thread(() -> {
            bankSimulatesByThreadJoin.withdraw(200);
            log.info("{} executed", Thread.currentThread().getName());
        }, "withDrawThread");

        depositThread.start();
        withDrawThread.start();
        try {
            depositThread.join();
            withDrawThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        log.info("The current balance: {}", bankSimulatesByThreadJoin.balance);
    }
}