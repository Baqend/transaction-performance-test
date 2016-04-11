import info.orestes.common.typesystem.ObjectId;
import info.orestes.common.typesystem.ObjectRef;
import operation.OperationContext;
import tracking.LatencyTracker;
import tracking.TransactionTracker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes the transaction performance test.
 */
public class TransactionPerformanceTest {
    private final Config config;
    private final Random rnd;
    private final AtomicInteger counter;


    public TransactionPerformanceTest(Config config) {
        this.config = config;
        this.rnd = new Random(config.getNumObject() + config.getTransactions() + config.getTransactionSize());
        counter = new AtomicInteger(0);
    }


    public void run(String resultPath, OperationContext context) {
        LatencyTracker latencyTracker = new LatencyTracker();
        TransactionTracker transactionTracker = new TransactionTracker();
        long start = System.nanoTime();

        final ExecutorService es = Executors.newFixedThreadPool(config.getConnections());
        CompletionService<Boolean> cs = new ExecutorCompletionService<>(es);

        for (int i = 0; i < config.getTransactions(); i++) {
            cs.submit(() -> transactionTask(context, latencyTracker));
        }

        for (int i = 0; i < config.getTransactions(); i++) {
            handleTransactionResult(transactionTracker, cs);
        }

        long runtime = System.nanoTime() - start;

        try {
            writeResults(resultPath, latencyTracker, transactionTracker, runtime);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void writeResults(String resultPath, LatencyTracker latencyTracker, TransactionTracker transactionTracker, long runtime) throws FileNotFoundException {
        File resultDir = new File("results");
        if (!resultDir.exists()) {
            resultDir.mkdir();
        }
        try (PrintWriter writer = new PrintWriter(resultDir.getPath() + "/" + resultPath)) {
            writer.println("Time " + (runtime / 1_000_000.0));
            writer.println("Transactions " + config.getTransactions());
            writer.println("Per Second " + (config.getTransactions() / (runtime / 1_000_000_000.0)));
            writer.println("Aborts " + transactionTracker.getAborted());
            writer.println("Errors " + transactionTracker.getErrors());
            writer.println("Latency (avg) " + latencyTracker.getAvgLatency() / 1_000_000);
        }
    }

    private void handleTransactionResult(TransactionTracker transactionTracker, CompletionService<Boolean> cs) {
        try {
            Future<Boolean> result = cs.take();
            Boolean success = result.get();
            if (success) {
                transactionTracker.reportCompletion();
            } else {
                transactionTracker.reportAbort();
            }
        } catch (Exception e) {
            transactionTracker.reportError();
        }
    }

    private Boolean transactionTask(OperationContext context, LatencyTracker latencyTracker) {
        long start = System.nanoTime();
        CompletableFuture<Void> transaction = executeTransaction(context);
        Boolean success = transaction.handle((res, err) -> err == null).join();
        long latency = System.nanoTime() - start;
        latencyTracker.trackLatency(latency);
        return success;
    }

    private CompletableFuture<Void> executeTransaction(OperationContext context) {
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
        return trContext.thenCompose(OperationContext.SequenceContext::commit);
    }


    private ObjectRef getRandomRef() {
        int key = rnd.nextInt(config.getNumObject());
        return ObjectRef.of(ConnectionSetup.TEST_BUCKET, new ObjectId(key));
    }

}
