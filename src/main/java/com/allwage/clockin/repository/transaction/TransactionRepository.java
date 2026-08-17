package com.allwage.clockin.repository.transaction;

import java.util.function.Supplier;

/**
 * Persistence port for an atomic operation that can span aggregate documents.
 */
public interface TransactionRepository {

    /**
     * Executes an operation atomically.
     *
     * @param operation operation to execute
     * @param <T> operation result type
     * @return operation result
     */
    <T> T executeAtomically(Supplier<T> operation);
}
