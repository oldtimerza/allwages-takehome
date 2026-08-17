package com.allwage.clockin.repository.clock;

import com.allwage.clockin.model.Site.ValidatedClockEvent;
import com.allwage.clockin.repository.store.DocumentStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Document-store implementation of clock event persistence.
 */
@Repository
public class ClockEventDocumentStoreRepository implements ClockEventRepository {

    private static final String COLLECTION = "clocks";

    private final DocumentStore store;

    public ClockEventDocumentStoreRepository(@NonNull DocumentStore store) {
        this.store = store;
    }

    @Override
    public void save(@NonNull ValidatedClockEvent clockEvent) {
        store.save(COLLECTION, clockEvent.clockEvent().id(), clockEvent);
    }

    @Override
    public @NonNull Optional<ValidatedClockEvent> findById(@NonNull String id) {
        return store.findById(COLLECTION, id, ValidatedClockEvent.class);
    }

    @Override
    public @NonNull List<ValidatedClockEvent> findAll() {
        return store.findAll(COLLECTION, ValidatedClockEvent.class);
    }
}
