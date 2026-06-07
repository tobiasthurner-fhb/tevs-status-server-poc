package com.statusserver.status.persistence;

import com.statusserver.status.domain.StatusMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository für aktuelle Statusmeldungen.
 */
public interface StatusMessageRepository extends JpaRepository<StatusMessage, String> {
    Optional<StatusMessage> findByUsername(String username);
}
