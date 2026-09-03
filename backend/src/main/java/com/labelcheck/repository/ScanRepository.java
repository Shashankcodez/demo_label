package com.labelcheck.repository;

import com.labelcheck.entity.ScanEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository providing persistence operations for label scan records.
 */
@Repository
public interface ScanRepository extends JpaRepository<ScanEntity, Long> {

    /**
     * Finds a persisted scan analysis by its public UUID.
     *
     * @param scanId the unique scan identifier
     * @return Optional containing ScanEntity if found
     */
    Optional<ScanEntity> findByScanId(UUID scanId);

    /**
     * Retrieves paginated scan history ordered newest-first by creation timestamp.
     *
     * @param pageable pagination parameters
     * @return Page of ScanEntity
     */
    Page<ScanEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
