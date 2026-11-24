package com.policystrike.simulation;

import com.policystrike.PolicyStrikeApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/** Optional command-line entry point for replaying persisted policy versions. */
public class PolicyVersionReplayDemo {

    public static void main(String[] args) {
        String policyId = args.length > 0 ? args[0] : "PS-001";

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                PolicyStrikeApplication.class)
                .web(WebApplicationType.NONE)
                .run(args)) {

            PolicyVersionReplayService service =
                    context.getBean(PolicyVersionReplayService.class);

            List<AttackReplayResult> results;
            if (args.length >= 3) {
                results = service.replay(policyId,
                        Integer.parseInt(args[1]),
                        Integer.parseInt(args[2]));
            } else {
                results = service.replayLatestPair(policyId);
            }

            if (results.isEmpty()) {
                System.out.println("At least two non-rejected persisted policy versions are required.");
                return;
            }

            for (AttackReplayResult result : results) {
                System.out.printf(
                        "%s | v%d approved=%d blocked=%d | v%d approved=%d blocked=%d | newly mitigated=%s%n",
                        result.attackType(),
                        result.baselineVersion(),
                        result.baselineAutoApprovedRequests(),
                        result.baselineBlockedRequests(),
                        result.candidateVersion(),
                        result.candidateAutoApprovedRequests(),
                        result.candidateBlockedRequests(),
                        result.remediationVerified()
                );
            }
        }
    }
}
