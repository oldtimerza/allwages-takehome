package com.allwage.clockin.repository.site;

import com.allwage.clockin.model.Site.Site;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Persistence port for site aggregate documents.
 */
public interface SiteRepository {

    /**
     * Saves a complete site aggregate document.
     *
     * @param site site to save
     */
    void save(Site site);

    /**
     * Saves a complete site aggregate document only when its identifier is unused.
     *
     * @param site site to create
     * @return true when the site was saved, or false when its identifier already exists
     */
    boolean saveIfAbsent(Site site);

    /**
     * Finds a site by identifier.
     *
     * @param id site identifier
     * @return the site when present
     */
    Optional<Site> findById(String id);

    /**
     * Applies an aggregate mutation and saves the resulting site as one operation.
     *
     * @param id site identifier
     * @param mutation aggregate mutation, or empty when its nested target does not exist
     * @return updated site when the site and nested target exist
     */
    Optional<Site> update(String id, Function<Site, Optional<Site>> mutation);

    /**
     * Finds the sites where an employee had an active assignment on a date.
     *
     * @param employeeId employee identifier
     * @param date device clock date in SAST
     * @return matching site aggregates
     */
    List<Site> findAssignedTo(String employeeId, LocalDate date);
}
