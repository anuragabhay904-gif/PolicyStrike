
package com.policystrike.api;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    @GetMapping("/health")
    public Map<String, String> health() {

        return Map.of(
                "application", "PolicyStrike",
                "status", "UP",
                "message", "PolicyStrike API is running"
        );
    }
}
