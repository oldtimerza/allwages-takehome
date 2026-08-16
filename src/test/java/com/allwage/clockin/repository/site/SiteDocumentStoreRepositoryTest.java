package com.allwage.clockin.repository.site;

import com.allwage.clockin.model.Site;
import com.allwage.clockin.model.SiteAssignment;
import com.allwage.clockin.model.Team;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SiteDocumentStoreRepositoryTest {

    private final DocumentStore store = new DocumentStore();
    private final SiteDocumentStoreRepository repository = new SiteDocumentStoreRepository(store);

    @AfterEach
    void clearStore() {
        store.clearCollection("sites");
    }

    @Test
    void findsSiteWithEmployeeAssignmentActiveOnDeviceDate() {
        Site assignedSite = new Site(
            "site-1",
            "Farm Alpha",
            null,
            List.of(new Team("team-1", "Harvest", null)),
            List.of(),
            List.of(new SiteAssignment("employee-1", "team-1", LocalDate.of(2026, 1, 1), null, null))
        );
        Site unassignedSite = new Site("site-2", "Farm Bravo", null, List.of(), List.of(), List.of());
        repository.save(assignedSite);
        repository.save(unassignedSite);

        assertThat(repository.findAssignedTo("employee-1", LocalDate.of(2026, 1, 15)))
            .containsExactly(assignedSite);
    }

    @Test
    void savesSiteOnlyWhenItsIdentifierIsUnused() {
        Site originalSite = new Site("site-1", "Farm Alpha", null, List.of(), List.of(), List.of());
        Site replacementSite = new Site("site-1", "Farm Bravo", null, List.of(), List.of(), List.of());

        boolean originalWasSaved = repository.saveIfAbsent(originalSite);
        boolean replacementWasSaved = repository.saveIfAbsent(replacementSite);

        assertThat(originalWasSaved).isTrue();
        assertThat(replacementWasSaved).isFalse();
        assertThat(repository.findById("site-1")).contains(originalSite);
    }
}
