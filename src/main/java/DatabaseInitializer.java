/**
 * Initializes the database.
 */
public class DatabaseInitializer {

    public static void main(String[] args) {
        int numObjects = 10_000;
        if (args.length > 0) {
            try {
                numObjects = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Not a valid integer, used default instead (" + numObjects + ")");
            }
        }

        ConnectionSetup connection = new ConnectionSetup("localhost");
        connection.initDatabase(numObjects);
        System.exit(0);
    }
}
