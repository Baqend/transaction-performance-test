package tracking;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

/**
 * Tracks latencies.
 */
public class LatencyTracker {

    private final ConcurrentLinkedQueue<Long> queue = new ConcurrentLinkedQueue<>();

    public void trackLatency(long latency) {
        queue.add(latency);
    }

    public double getAvgLatency() {
        return queue.stream().mapToLong(l -> l).average().getAsDouble();
    }
}
