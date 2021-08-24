package org.olxscrapper.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/health-check")
    public ResponseEntity<Void> HealthCheck() {
        return ResponseEntity.ok().build();
    }
}
