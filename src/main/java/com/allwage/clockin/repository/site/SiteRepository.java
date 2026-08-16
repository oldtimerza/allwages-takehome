package com.allwage.clockin.repository.site;

import com.allwage.clockin.model.Site;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Finds a site by identifier.
     *
     * @param id site identifier
     * @return the site when present
     */
    Optional<Site> findById(String id);

    /**
     * Finds the sites where an employee had an active assignment on a date.
     *
     * @param employeeId employee identifier
     * @param date device clock date in SAST
     * @return matching site aggregates
     */
    List<Site> findAssignedTo(String employeeId, LocalDate date);
}
