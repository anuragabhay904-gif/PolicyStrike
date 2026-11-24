package com.policystrike.api;

import com.policystrike.simulation.AttackReplayResult;
import com.policystrike.simulation.PolicyVersionReplayService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/replays")
public class PolicyReplayController {

    private final PolicyVersionReplayService replayService;

    public PolicyReplayController(PolicyVersionReplayService replayService) {
        this.replayService = replayService;
    }

    /** Compares the two newest non-rejected persisted versions. */
    @PostMapping("/run")
    public List<AttackReplayResult> runLatestReplay(
            @RequestParam(defaultValue = "PS-001") String policyId) {
        return replayService.replayLatestPair(policyId);
    }

    /** Allows a reviewer to compare an explicit stored baseline and candidate. */
    @PostMapping("/{policyId}/run")
    public List<AttackReplayResult> runVersionReplay(
            @PathVariable String policyId,
            @RequestParam int baselineVersion,
            @RequestParam int candidateVersion) {
        return replayService.replay(policyId, baselineVersion, candidateVersion);
    }
}
