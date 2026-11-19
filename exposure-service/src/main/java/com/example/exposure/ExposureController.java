package com.example.exposure;

import com.example.exposure.ExposureService.ExposureSummary;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/exposure")
public class ExposureController {
    private final ExposureService service;

    public ExposureController(ExposureService service) { this.service = service; }

    /** Manager dashboard: outstanding liability overall and per market. Eventually consistent. */
    @GetMapping("/summary")
    public ExposureSummary summary(@RequestHeader("X-User-Role") String role) {
        if (!"MANAGER".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Managers only");
        }
        return service.summary();
    }
}
