package com.statusserver.status.api;

import com.statusserver.status.application.StatusService;
import com.statusserver.status.application.dto.StatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-Controller für Statusmeldungen aus Sicht externer Clients.
 */
@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {
    private final StatusService statusService;

    /**
     * Liefert alle aktuell bekannten Statusmeldungen dieser Node.
     *
     * @return Liste aller Statusmeldungen
     */
    @GetMapping
    public List<StatusDto> listAll() {
        return statusService.findAll();
    }

    /**
     * Liefert die Statusmeldung eines bestimmten Benutzers.
     *
     * @param username eindeutiger Benutzername
     * @return Statusmeldung oder 404, wenn kein Eintrag existiert
     */
    @GetMapping("/{username}")
    public ResponseEntity<StatusDto> getOne(@PathVariable String username) {
        return statusService.findOne(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Erstellt oder aktualisiert eine Statusmeldung.
     * Nur konfliktfrei übernommene Änderungen werden repliziert.
     *
     * @param request eingehende Statusmeldung
     * @return gespeicherte Statusmeldung
     */
    @PostMapping
    public ResponseEntity<StatusDto> upsert(@RequestBody StatusDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.upsertFromClient(request));
    }

    /**
     * Löscht die Statusmeldung eines Benutzers und repliziert die Löschung.
     *
     * @param username eindeutiger Benutzername
     * @return leere Antwort bei erfolgreicher Verarbeitung
     */
    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable String username) {
        statusService.deleteFromClient(username);
        return ResponseEntity.noContent().build();
    }
}
