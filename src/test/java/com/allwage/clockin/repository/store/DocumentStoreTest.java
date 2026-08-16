package com.allwage.clockin.repository.store;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentStoreTest {

    @Test
    void executeAtomically_restoresDocumentsWhenOperationFails() {
        DocumentStore store = new DocumentStore();

        assertThatThrownBy(() -> store.executeAtomically(() -> {
            store.save("clocks", "clock-123", "clock");
            store.save("clock-audits", "audit-123", "audit");
            throw new IllegalStateException("Audit write failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.findAll("clocks", String.class)).isEmpty();
        assertThat(store.findAll("clock-audits", String.class)).isEmpty();
    }

    @Test
    void executeAtomically_preservesExistingDocumentsWhenOperationFails() {
        DocumentStore store = new DocumentStore();
        store.save("clock-audits", "audit-existing", "original-audit");

        assertThatThrownBy(() -> store.executeAtomically(() -> {
            store.save("clock-audits", "audit-existing", "replacement-audit");
            throw new IllegalStateException("Audit write failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(store.findById("clock-audits", "audit-existing", String.class))
            .contains("original-audit");
    }
}
