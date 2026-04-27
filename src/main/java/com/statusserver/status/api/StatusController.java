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

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
public class StatusController {
    private final StatusService statusService;

    @GetMapping
    public List<StatusDto> listAll() {
        return statusService.findAll();
    }

    @GetMapping("/{username}")
    public ResponseEntity<StatusDto> getOne(@PathVariable String username) {
        return statusService.findOne(username)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<StatusDto> upsert(@RequestBody StatusDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(statusService.upsertFromClient(request));
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> delete(@PathVariable String username) {
        statusService.deleteFromClient(username);
        return ResponseEntity.noContent().build();
    }
}
