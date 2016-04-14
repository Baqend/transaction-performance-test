package tracking;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * Tracks latencies.
 */
public class LatencyTracker {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    /**
     * Tracks a single latency value.
     *
     * @param latency The latency value in nanoseconds.
     */
    public void trackLatency(long latency) {
        queue.add(latency);
    }

    /**
     * Returns the average latency in nanoseconds.
     *
     * @return The average latency in nanoseconds.
     */
    public double getAvgLatency() {
        return queue.stream().mapToLong(l -> l).average().getAsDouble();
    }
}
