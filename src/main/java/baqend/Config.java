package baqend;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

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
     * The attempted throughput (number of transactions) per client per second
     */
    private int targetThroughput = 300;
    /**
     * Sets the maximum number of parallel transaction for this client.
     * Might impact target throughput rate.
     */
    private int maxClientParallelism = 10;

    public int getMaxClientParallelism() {
        return maxClientParallelism;
    }

    public void setMaxClientParallelism(int maxClientParallelism) {
        this.maxClientParallelism = maxClientParallelism;
    }

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

    public int getTargetThroughput() {
        return targetThroughput;
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

    public void setTargetThroughput(int targetThroughput) {
        this.targetThroughput = targetThroughput;
    }

    public static Config readFromFile(String path) throws IOException {
        Gson gson = new Gson();

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            return gson.fromJson(reader, Config.class);
        }
    }
}
