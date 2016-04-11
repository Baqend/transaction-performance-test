package operation;

import info.orestes.client.OrestesClient;
import info.orestes.client.TransactionClient;
import info.orestes.common.typesystem.ObjectRef;
import info.orestes.pluggable.types.data.OObject;

import java.util.concurrent.CompletableFuture;

/**
 * A transactional context.
 */
public class TransactionContext implements OperationContext {
    protected final OrestesClient client;

    public TransactionContext(OrestesClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<Transaction> begin() {
        CompletableFuture<TransactionClient> transactionFuture = client.beginTransactionWithAsyncClient();
        return transactionFuture.thenApply(transaction -> new Transaction(transaction));
    }

    public class Transaction implements SequenceContext {
        protected final TransactionClient transaction;

        public Transaction(TransactionClient transaction) {
            this.transaction = transaction;
        }

        @Override
        public CompletableFuture<OObject> read(ObjectRef ref) {
            return transaction.loadAsync(ref);
        }

        @Override
        public CompletableFuture<Void> update(ObjectRef ref) {
            return read(ref).thenAccept(obj -> {
                Long val = obj.getValue("value");
                obj.setValue("value", val + 1);
                transaction.update(obj);
            });
        }

        @Override
        public CompletableFuture<Void> commit() {
            return transaction.commitAsync().thenAccept(ignored -> {
            });
        }

    }
}
