import operation.PartialUpdateContext;
import operation.StandardContext;
import operation.TransactionContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Starts the performance test.
 */
public class Starter {

    public static void main(String[] args) {
        String ip;
        if (args.length > 0) {
            ip = args[0];
        } else {
            ip = "localhost";
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        long startLong = nextMinute.toInstant(ZoneOffset.UTC).toEpochMilli();

        long nowLong = now.toInstant(ZoneOffset.UTC).toEpochMilli();
        long delta = startLong - nowLong;

//        if (delta < 5_000) {
//            throw new RuntimeException("Dangerous scheduling");
//        }
        delta = 0;
        System.out.println("Waiting...");
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
        executor.schedule(() -> run(ip), delta, TimeUnit.MILLISECONDS);
    }

    private static void run(String ip) {
        System.out.println("Time " + LocalDateTime.now());
        System.out.println("Running...");
        Config config = new Config();
        ConnectionSetup setup = new ConnectionSetup(ip);
        // uncomment to init database (do not use with multiple clients) use DatabaseInitializer instead.
        setup.initDatabase(config.getNumObject());
        System.out.println("Initialized...");

        TransactionPerformanceTest test = new TransactionPerformanceTest(config);

        test.run("warmup.dat", new StandardContext(setup.getClient()));
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
