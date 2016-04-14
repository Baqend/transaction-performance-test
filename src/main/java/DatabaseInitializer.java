import baqend.ConnectionSetup;

/**
 * Initializes the database.
 */
public class DatabaseInitializer {

    public static void main(String[] args) {
        String ip = "localhost";
        int numObjects = 10_000;
        if (args.length > 0) {
            ip = args[0];
            if (args.length > 1) {
                try {
                    numObjects = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Not a valid integer, used default instead (" + numObjects + ")");
                }
            }
        }

        ConnectionSetup connection = new ConnectionSetup(ip);
        connection.initDatabase(numObjects);
        System.exit(0);
    }
}
