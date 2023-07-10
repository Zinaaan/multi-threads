package sequentialExecution;

/**
 * @author lzn
 * @date 2023/07/08 21:40
 * @description
 */
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
            System.out.println(Thread.currentThread().getName() + " executed");
        }, "depositThread");
        Thread withDrawThread = new Thread(() -> {
            bankSimulatesByThreadJoin.withdraw(200);
            System.out.println(Thread.currentThread().getName() + " executed");
        }, "withDrawThread");

        depositThread.start();
        withDrawThread.start();
        try {
            depositThread.join();
            withDrawThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("The current balance: " + bankSimulatesByThreadJoin.balance);
    }
}