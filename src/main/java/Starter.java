import operation.PartialUpdateContext;
import operation.StandardContext;
import operation.TransactionContext;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Starts the performance test.
 */
public class Starter {

    public static void main(String[] args) {
        long offset = 0; // TODO calculate offset to sync multiple clients.
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> run(), offset, TimeUnit.MILLISECONDS);
    }

    private static void run() {
        System.out.println("Running...");
        Config config = new Config();
        ConnectionSetup setup = new ConnectionSetup("localhost");
        setup.initDatabase(config.getNumObject());
        System.out.println("Initialized...");

        TransactionPerformanceTest test = new TransactionPerformanceTest(config);

        test.run("standard.dat", new StandardContext(setup.getClient()));
        System.out.println("Standard...done.");
        test.run("transactional.dat", new TransactionContext(setup.getClient()));
        System.out.println("transactional...done.");
        test.run("partialUpdate.dat", new PartialUpdateContext(setup.getClient(), setup.getValueClassField()));
        System.out.println("partialupdate...done.");
        System.out.println("exit.");
        System.exit(0);
    }

}
