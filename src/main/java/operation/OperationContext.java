package operation;

import info.orestes.common.typesystem.ObjectRef;
import info.orestes.pluggable.types.data.OObject;

import java.util.concurrent.CompletableFuture;

/**
 * The interface operation contexts.
 */
public interface OperationContext {

    /**
     * Begins the sequence of operations and returns the (updated) context.
     */
    CompletableFuture<? extends SequenceContext> begin();


    interface SequenceContext {
        /**
         * Load the specified object.
         *
         * @param ref The object reference.
         * @return A future containing the object.
         */
        CompletableFuture<OObject> read(ObjectRef ref);

        /**
         * Randomly update the specified object.
         *
         * @param ref The object reference.
         * @return A future that completes after the update.
         */
        CompletableFuture<Void> update(ObjectRef ref);

        /**
         * Commits / finishes the operation sequence.
         *
         * @return A future that completes after the commit.
         */
        CompletableFuture<Void> commit();
    }

}
