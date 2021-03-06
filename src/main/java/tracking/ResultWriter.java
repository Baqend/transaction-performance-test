package tracking;

import baqend.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Writes the results to files.
 */
public class ResultWriter {
    /**
     * Writes the results to the specified file.
     *
     * @param config         The config.
     * @param resultPath     The path of the file to write to.
     * @param latencyTracker The latency tracker.
     * @param successes      The number of successful transactions.
     * @param runtime        The runtime of all transactions combined.
     */
    public static void writeResults(Config config, String resultPath, LatencyTracker latencyTracker, long successes, long runtime) {
        File resultDir = new File("results");
        if (!resultDir.exists()) {
            resultDir.mkdir();
        }
        try (PrintWriter writer = new PrintWriter(resultDir.getPath() + "/" + resultPath)) {
            writer.println("Time " + (runtime / 1_000_000.0));
            writer.println("Transactions " + config.getTransactions());
            writer.println("Per Second " + (config.getTransactions() / (runtime / 1_000_000_000.0)));
            writer.println("Aborts " + (config.getTransactions() - successes));
            writer.println("Latency (avg) " + (latencyTracker.getAvgLatency() / 1_000_000));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
