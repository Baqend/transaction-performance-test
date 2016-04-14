package baqend;

import com.google.common.util.concurrent.RateLimiter;
import info.orestes.common.typesystem.ObjectId;
import info.orestes.common.typesystem.ObjectRef;
import operation.OperationContext;
import tracking.LatencyTracker;
import tracking.ResultWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

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

    /**
     * Runs the performance test with the given context.
     *
     * @param resultPath The path to write the results to.
     * @param context    The context to execute the transaction with.
     */
    public void run(String resultPath, OperationContext context) {
        LatencyTracker latencyTracker = new LatencyTracker();
        RateLimiter rateLimiter = RateLimiter.create(config.getTargetThroughput());
        Semaphore semaphore = new Semaphore(config.getMaxClientParallelism());

        long start = System.nanoTime();
        List<CompletableFuture<Boolean>> transactions = new ArrayList<>(config.getTransactions());
        for (int i = 0; i < config.getTransactions(); i++) {
            rateLimiter.acquire();
            CompletableFuture<Boolean> transaction = acquireSlot(semaphore)
                    .thenCompose(ignored -> executeTransaction(context, latencyTracker))
                    .thenApply(success -> {
                        semaphore.release();
                        return success;
                    });
            transactions.add(transaction);
        }

        long successes = transactions.stream().filter(CompletableFuture::join).count();
        long runtime = System.nanoTime() - start;

        ResultWriter.writeResults(config, resultPath, latencyTracker, successes, runtime);
    }

    /**
     * Acquires a slot for the transaction execution.
     * This limits the number of concurrent transactions in this client by so the response processing does not queue up.
     *
     * @param semaphore The semaphore that limit the concurrency.
     * @return A future that is complete as soon as the semaphore can acquire a slot.
     */
    private CompletableFuture<Void> acquireSlot(Semaphore semaphore) {
        return CompletableFuture.runAsync(() -> {
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Executes a full transaction from begin to commit by composing futures.
     *
     * @param context        The context used to excute the transaction.
     * @param latencyTracker An object to track the latency of the transaction in.
     * @return A future representing the running transaction.
     */
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

    /**
     * Returns a random object reference.
     *
     * @return A random object reference.
     */
    private ObjectRef getRandomRef() {
        int key = rnd.nextInt(config.getNumObject());
        return ObjectRef.of(ConnectionSetup.TEST_BUCKET, new ObjectId(key));
    }

}
