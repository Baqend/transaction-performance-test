package operation;

import info.orestes.client.OrestesClient;
import info.orestes.client.TransactionClient;
import info.orestes.common.typesystem.ObjectRef;
import info.orestes.common.typesystem.UpdateOperation;
import info.orestes.pluggable.types.data.DBClassField;

import java.util.concurrent.CompletableFuture;

/**
 * A transactional context that uses partial updates.
 */
public class PartialUpdateContext extends TransactionContext {
    private final DBClassField fieldToUpdate;

    public PartialUpdateContext(OrestesClient client, DBClassField fieldToUpdate) {
        super(client);
        this.fieldToUpdate = fieldToUpdate;
    }

    @Override
    public CompletableFuture<Transaction> begin() {
        CompletableFuture<TransactionClient> transactionFuture = client.beginTransactionWithAsyncClient();
        return transactionFuture.thenApply(transaction -> new Transaction(transaction));
    }

    class PartialUpdateTransaction extends Transaction {

        public PartialUpdateTransaction(TransactionClient transaction) {
            super(transaction);
        }

        @Override
        public CompletableFuture<Void> update(ObjectRef ref) {
            UpdateOperation update = new UpdateOperation("value", UpdateOperation.Operation.add, fieldToUpdate, 1);
            transaction.partialUpdate(ref, update);
            return CompletableFuture.completedFuture(null);
        }
    }

}
