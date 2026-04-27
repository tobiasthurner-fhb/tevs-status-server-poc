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

@Entity
@Table(name = "status_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusMessage {
    @Id
    @Column(nullable = false, updatable = false)
    private String username;

    @Column(nullable = false)
    private String statustext;

    @Column(nullable = false)
    private OffsetDateTime uhrzeit;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;
}
