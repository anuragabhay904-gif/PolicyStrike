
package com.policystrike.api;

import com.policystrike.ai.AttackScenarioGenerationService;
import com.policystrike.ai.PolicyCompilationService;
import com.policystrike.ai.VulnerabilityExplanationService;

import com.policystrike.api.dto.AttackScenarioExecutionResponse;
import com.policystrike.api.dto.CompilePolicyRequest;
import com.policystrike.api.dto.PolicyCompilationResponse;
import com.policystrike.api.dto.GenerateAttackScenariosRequest;
import com.policystrike.api.dto.GeneratedAttackScenariosResponse;
import com.policystrike.api.dto.VulnerabilityExplanationResponse;

import com.policystrike.persistence.service.PolicyVersionService;
import com.policystrike.simulation.GeneratedScenarioExecutionService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class GeminiController {

    private final PolicyCompilationService compilationService;
    private final AttackScenarioGenerationService scenarioService;
    private final VulnerabilityExplanationService explanationService;
    private final PolicyVersionService policyVersionService;
    private final GeneratedScenarioExecutionService scenarioExecutionService;

    public GeminiController(
            PolicyCompilationService compilationService,
            AttackScenarioGenerationService scenarioService,
            VulnerabilityExplanationService explanationService,
            PolicyVersionService policyVersionService,
            GeneratedScenarioExecutionService scenarioExecutionService) {

        this.compilationService = compilationService;
        this.scenarioService = scenarioService;
        this.explanationService = explanationService;
        this.policyVersionService = policyVersionService;
        this.scenarioExecutionService = scenarioExecutionService;
    }

    // Feature 1: Natural-language policy compilation.
    @PostMapping("/policies/compile")
    public PolicyCompilationResponse compilePolicy(
            @RequestBody CompilePolicyRequest request) {

        return compilationService.compile(request);
    }

    // Feature 2: Generate adversarial attack scenarios.
    @PostMapping("/policies/{policyId}/scenarios/generate")
    public GeneratedAttackScenariosResponse generateScenarios(
            @PathVariable String policyId,
            @RequestBody GenerateAttackScenariosRequest request) {

        var policyEntity =
                policyVersionService.getActiveVersion(policyId);

        return scenarioService.generate(
                policyEntity.toDomain(),
                request
        );
    }

    // Feature 3: Execute previously generated attack scenarios using Java rules.
    @PostMapping("/policies/{policyId}/scenarios/execute")
    public AttackScenarioExecutionResponse executeScenarios(
            @PathVariable String policyId,
            @RequestBody GeneratedAttackScenariosResponse generated) {

        var policyEntity =
                policyVersionService.getActiveVersion(policyId);

        return scenarioExecutionService.execute(
                policyEntity,
                generated
        );
    }

    // Feature 4: Explain deterministic replay findings.
    @PostMapping("/vulnerabilities/explain")
    public VulnerabilityExplanationResponse explainVulnerabilities() {

        return explanationService.explain();
    }
}
