package com.statusserver.status.persistence;

import com.statusserver.status.domain.StatusMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusMessageRepository extends JpaRepository<StatusMessage, String> {
}
