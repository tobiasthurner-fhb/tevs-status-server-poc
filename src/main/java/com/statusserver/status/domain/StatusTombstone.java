package com.statusserver.status.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Persistierte Löschmarke, damit verpasste Delete-Events bei späterem Sync nicht verloren gehen.
 */
@Entity
@Table(name = "status_tombstones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusTombstone {
    @Id
    @Column(nullable = false, updatable = false)
    private String username;

    @Column(nullable = false)
    private OffsetDateTime deletedAt;
}
