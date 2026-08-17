package com.allwage.clockin.repository.transaction;

import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionDocumentStoreRepositoryTest {

    @Test
    void executeAtomically_restoresDocumentsWhenOperationFails() {
        // Given
        DocumentStore store = new DocumentStore();
        TransactionDocumentStoreRepository repository = new TransactionDocumentStoreRepository(store);

        // When / Then
        assertThatThrownBy(() -> repository.executeAtomically(() -> {
            store.save("clocks", "clock-1", "clock event");
            throw new IllegalStateException("Cannot persist audit event");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.findById("clocks", "clock-1", String.class)).isEmpty();
    }
}
