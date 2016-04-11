package operation;

import info.orestes.client.OrestesClient;
import info.orestes.common.typesystem.ObjectRef;
import info.orestes.pluggable.types.data.OObject;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * A non-transactional context.
 */
public class StandardContext implements OperationContext {
    private final OrestesClient client;

    public StandardContext(OrestesClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<? extends SequenceContext> begin() {
        return CompletableFuture.completedFuture(new Batch());
    }

    class Batch implements SequenceContext {

        @Override
        public CompletableFuture<OObject> read(ObjectRef ref) {
            return client.loadAllInfos(Stream.of(ref)).findFirst().get();
        }

        @Override
        public CompletableFuture<Void> update(ObjectRef ref) {
            return read(ref).thenCompose(obj -> {
                Long val = obj.getValue("value");
                obj.setValue("value", val + 1);
                // make sure that a single rejected update does not abort the whole batch.
                return client.updateAllObjects(Stream.of(obj)).findFirst().get()
                        .handle((res, err) -> null).thenAccept(ignored -> {
                        });
            });
        }

        @Override
        public CompletableFuture<Void> commit() {
            return CompletableFuture.completedFuture(null);
        }
    }
}
