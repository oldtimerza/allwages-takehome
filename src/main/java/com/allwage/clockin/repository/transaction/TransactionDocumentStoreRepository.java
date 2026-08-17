package com.allwage.clockin.repository.transaction;

import com.allwage.clockin.repository.store.DocumentStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.function.Supplier;

/**
 * Document-store implementation of atomic persistence operations.
 */
@Repository
public class TransactionDocumentStoreRepository implements TransactionRepository {

    private final DocumentStore store;

    public TransactionDocumentStoreRepository(@NonNull DocumentStore store) {
        this.store = store;
    }

    @Override
    public @NonNull <T> T executeAtomically(@NonNull Supplier<T> operation) {
        return store.executeAtomically(operation);
    }
}
