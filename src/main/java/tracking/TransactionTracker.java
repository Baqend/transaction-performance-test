package tracking;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks the status of transaction executions.
 */
public class TransactionTracker {

    private final AtomicInteger completed = new AtomicInteger();
    private final AtomicInteger aborted = new AtomicInteger();
    private final AtomicInteger error = new AtomicInteger();

    public void reportCompletion() {
        completed.incrementAndGet();
    }

    public void reportAbort() {
        aborted.incrementAndGet();
    }

    public void reportError() {
        error.incrementAndGet();
    }

    public int getCompleted() {
        return completed.get();
    }

    public int getAborted() {
        return aborted.get();
    }

    public int getErrors() {
        return error.get();
    }
}
