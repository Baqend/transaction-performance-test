import java.util.Random;

/**
 * Configures the performance test.
 */
public class Config {
    /**
     * The number of objects used in the test run.
     */
    private int numObject = 10_000;
    /**
     * The number of operation sequences executed in the text run.
     * e.g. the number of transactions.
     */
    private int transactions = 10_000;
    /**
     * The size of the executed transactions.
     */
    private int transactionSize = 10;
    /**
     * The rate of objects that are read without writing them.
     * e.g. 0.8 read rate and transaction size 10 means 8 objects are read and 2 objects are written
     * (and probably read before that).
     */
    private double readRate = 0.80;
    /**
     * The number of connections used by the client (i.e. the number of parallel transactions).
     */
    private int connections = 5;


    public int getNumObject() {
        return numObject;
    }

    public int getTransactions() {
        return transactions;
    }

    public int getTransactionSize() {
        return transactionSize;
    }

    public double getReadRate() {
        return readRate;
    }

    public int getConnections() {
        return connections;
    }

    public void setNumObject(int numObject) {
        this.numObject = numObject;
    }

    public void setTransactions(int transactions) {
        this.transactions = transactions;
    }

    public void setTransactionSize(int transactionSize) {
        this.transactionSize = transactionSize;
    }

    public void setReadRate(double readRate) {
        this.readRate = readRate;
    }

    public void setConnections(int connections) {
        this.connections = connections;
    }
}
