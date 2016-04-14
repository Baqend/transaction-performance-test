package baqend;

import com.google.common.util.concurrent.RateLimiter;
import info.orestes.common.typesystem.ObjectId;
import info.orestes.common.typesystem.ObjectRef;
import operation.OperationContext;
import tracking.LatencyTracker;
import tracking.ResultWriter;

import java.io.FileNotFoundException;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Executes the transaction performance test.
 */
public class TransactionPerformanceTest {
    private final Config config;
    private final Random rnd;
    private final CompletionService<Boolean> csTransaction;

    public TransactionPerformanceTest(Config config) {
        this.config = config;
        this.rnd = new Random(config.getNumObject() + config.getTransactions() + config.getTransactionSize());
        ExecutorService es = Executors.newFixedThreadPool(10);
        csTransaction = new ExecutorCompletionService<>(es);
    }

    public void run(String resultPath, OperationContext context) {
        LatencyTracker latencyTracker = new LatencyTracker();
        RateLimiter rateLimiter = RateLimiter.create(config.getTargetThroughput());

        long start = System.nanoTime();
        for (int i = 0; i < config.getTransactions(); i++) {
            rateLimiter.acquire();
            csTransaction.submit(() -> executeTransaction(context, latencyTracker).join());
        }

        long successes = 0;
        for (int i = 0; i < config.getTransactions(); i++) {
            try {
                boolean success = csTransaction.take().get();
                if (success) {
                    successes++;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        try {
            long runtime = System.nanoTime() - start;
            ResultWriter.writeResults(config, resultPath, latencyTracker, successes, runtime);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
