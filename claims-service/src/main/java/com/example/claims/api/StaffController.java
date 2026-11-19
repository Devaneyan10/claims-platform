package com.example.claims.api;

import com.example.claims.domain.Role;
import com.example.claims.service.Caller;
import com.example.claims.service.WorkloadService;
import com.example.claims.service.WorkloadService.OfficerWorkload;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff")
public class StaffController {
    private final WorkloadService workload;

    public StaffController(WorkloadService workload) { this.workload = workload; }

    /** Officers see their own numbers; managers see every officer. */
    @GetMapping("/workload")
    public List<OfficerWorkload> workload(@RequestHeader("X-User-Id") String u, @RequestHeader("X-User-Role") Role r) {
        return workload.workload(new Caller(u, r));
    }
}
