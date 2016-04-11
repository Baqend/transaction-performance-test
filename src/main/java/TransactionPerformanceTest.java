import com.google.common.util.concurrent.RateLimiter;
import info.orestes.common.typesystem.ObjectId;
import info.orestes.common.typesystem.ObjectRef;
import operation.OperationContext;
import tracking.LatencyTracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * Executes the transaction performance test.
 */
public class TransactionPerformanceTest {
    private final Config config;
    private final Random rnd;

    public TransactionPerformanceTest(Config config) {
        this.config = config;
        this.rnd = new Random(config.getNumObject() + config.getTransactions() + config.getTransactionSize());
    }


    public void run(String resultPath, OperationContext context) {
        LatencyTracker latencyTracker = new LatencyTracker();
        RateLimiter rateLimiter = RateLimiter.create(config.getTargetThroughput());
        List<CompletableFuture<Boolean>> transactions = new ArrayList<>(config.getTransactions());

        long start = System.nanoTime();
        for (int i = 0; i < config.getTransactions(); i++) {
            rateLimiter.acquire();
            CompletableFuture<Boolean> transaction = executeTransaction(context, latencyTracker);
            transactions.add(transaction);
        }

        long successes = transactions.stream().filter(CompletableFuture::join).count();
        long runtime = System.nanoTime() - start;

        try {
            writeResults(resultPath, latencyTracker, successes, runtime);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeResults(String resultPath, LatencyTracker latency, long successes, long runtime) throws FileNotFoundException {
        File resultDir = new File("results");
        if (!resultDir.exists()) {
            resultDir.mkdir();
        }
        try (PrintWriter writer = new PrintWriter(resultDir.getPath() + "/" + resultPath)) {
            writer.println("Time " + (runtime / 1_000_000.0));
            writer.println("Transactions " + config.getTransactions());
            writer.println("Per Second " + (config.getTransactions() / (runtime / 1_000_000_000.0)));
            writer.println("Aborts " + (config.getTransactions() - successes));
            writer.println("Latency (avg) " + (latency.getAvgLatency() / 1_000_000));
        }
    }

    private CompletableFuture<Boolean> executeTransaction(OperationContext context, LatencyTracker latencyTracker) {
        long start = System.nanoTime();

        CompletableFuture<? extends OperationContext.SequenceContext> trContext = context.begin();
        for (int j = 0; j < config.getTransactionSize(); j++) {
            ObjectRef ref = getRandomRef();
            trContext = trContext.thenCompose(transaction -> {
                if (rnd.nextDouble() < config.getReadRate()) {
                    // read
                    return transaction.read(ref)
                            .thenApply(ignored -> transaction);
                } else {
                    // write
                    return transaction.update(ref)
                            .thenApply(ignored -> transaction);
                }
            });

        }
        CompletableFuture<Void> commit = trContext.thenCompose(OperationContext.SequenceContext::commit);

        return commit.handle((res, err) -> {
            latencyTracker.trackLatency(System.nanoTime() - start);
            return err == null;
        });
    }


    private ObjectRef getRandomRef() {
        int key = rnd.nextInt(config.getNumObject());
        return ObjectRef.of(ConnectionSetup.TEST_BUCKET, new ObjectId(key));
    }

}
