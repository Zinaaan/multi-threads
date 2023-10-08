package sequentialExecution;

import java.util.concurrent.Semaphore;

/**
 * @author lzn
 * @date 2023/07/09 13:34
 * @description
 */
public class BankSimulatesBySemaphore {

    private int balance;

    public BankSimulatesBySemaphore() {
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
        BankSimulatesBySemaphore bankSimulatesBySemaphore = new BankSimulatesBySemaphore();
        Semaphore semaphore1 = new Semaphore(0);
        Semaphore semaphore2 = new Semaphore(0);
        Thread depositThread = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " executed");
            bankSimulatesBySemaphore.deposit(500);
            semaphore2.release();
        }, "depositThread");

        Thread withdrawThread = new Thread(() -> {
            try {
                semaphore2.acquire();
                System.out.println(Thread.currentThread().getName() + " executed");
                bankSimulatesBySemaphore.withdraw(200);
                semaphore1.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "withdrawThread");

        depositThread.start();
        withdrawThread.start();
        try {
            semaphore1.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("The current balance: " + bankSimulatesBySemaphore.balance);
    }
}
