package baqend;

import info.orestes.client.OrestesClient;
import info.orestes.common.typesystem.*;
import info.orestes.pluggable.types.data.DBClassField;
import info.orestes.pluggable.types.data.OObject;
import info.orestes.rest.conversion.ClassFieldSpecification;
import info.orestes.rest.conversion.ClassSpecification;

/**
 * Sets up a connection to the baqend server using the java client api and initializes a schema.
 */
public class ConnectionSetup {
    public static final Bucket TEST_BUCKET = new Bucket("test.bucket.Value");
    private final DBClassField valueClassField;

    private final OrestesClient client;
    private final UserInfo rootUser;

    /**
     * Establishes a new connection to the given ip and ensures the schema exists.
     *
     * @param ip
     */
    public ConnectionSetup(String ip) {
        // Connect a client to the running baqend server using api version v1
        client = new OrestesClient("http://" + ip + ":8080/v1");

        // Set the root user with its credentials
        rootUser = client.login(new UserLogin("root", "root")).getSignedUserInfo();

        // A class specification with two fields
        ClassSpecification s = new ClassSpecification(TEST_BUCKET, BucketAcl.createDefault(),
                // CAUTION! Even though the type is named integer you have to store and load long values!!!
                new ClassFieldSpecification("value", Bucket.INTEGER),
                new ClassFieldSpecification("name", Bucket.STRING));

        // Add the schema for the test bucket on the server side.
        client.getSchema().add(s, rootUser);

        // Get class field for the object value (needed for partial updates)
        valueClassField = client.getSchema().getClass(TEST_BUCKET).getField("value");
    }

    /**
     * Returns the client to use for requests against the server.
     *
     * @return The client to use for requests against the server.
     */
    public OrestesClient getClient() {
        return client;
    }

    public void initDatabase(int numObjects) {
        client.truncateBucket(TEST_BUCKET, rootUser);
        for (int i = 0; i < numObjects; i++) {
            ObjectRef ref = ObjectRef.of(TEST_BUCKET, new ObjectId(i));
            OObject object = client.getSchema()
                    .getClass(ref.getBucket())
                    .newInstance(ref, ObjectAcl.createDefault());

            object.setValue("name", "Object-" + i);
            object.setValue("value", (long) 0);

            // Insert a single object into the database (non transactional)
            client.insert(object);
        }
    }

    /**
     * Returns the class field of the value field (from the example schema).
     *
     * @return The class field of the value field (from the example schema).
     */
    public DBClassField getValueClassField() {
        return valueClassField;
    }
}
