package com.statusserver.status.persistence;

import com.statusserver.status.domain.StatusTombstone;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository für Delete-Tombstones.
 */
public interface StatusTombstoneRepository extends JpaRepository<StatusTombstone, String> {
}
