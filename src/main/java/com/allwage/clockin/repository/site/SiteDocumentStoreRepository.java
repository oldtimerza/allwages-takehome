package com.allwage.clockin.repository.site;

import com.allwage.clockin.model.Site;
import com.allwage.clockin.repository.store.DocumentStore;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Document-store implementation of site aggregate persistence.
 */
@Repository
public class SiteDocumentStoreRepository implements SiteRepository {

    private static final String COLLECTION = "sites";

    private final DocumentStore store;

    public SiteDocumentStoreRepository(@NonNull DocumentStore store) {
        this.store = store;
    }

    @Override
    public void save(@NonNull Site site) {
        store.save(COLLECTION, site.id(), site);
    }

    @Override
    public @NonNull Optional<Site> findById(@NonNull String id) {
        return store.findById(COLLECTION, id, Site.class);
    }

    @Override
    public @NonNull Optional<Site> update(
        @NonNull String id,
        @NonNull Function<Site, Optional<Site>> mutation
    ) {
        return store.executeAtomically(() -> store.findById(COLLECTION, id, Site.class)
            .flatMap(site -> mutation.apply(site))
            .map(updatedSite -> {
                store.save(COLLECTION, id, updatedSite);
                return updatedSite;
            }));
    }

    @Override
    public @NonNull List<Site> findAssignedTo(@NonNull String employeeId, @NonNull LocalDate date) {
        return store.findAll(COLLECTION, Site.class).stream()
            .filter(site -> site.assignmentFor(employeeId, date).isPresent())
            .toList();
    }
}
