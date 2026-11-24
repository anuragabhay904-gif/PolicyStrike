package com.policystrike.api;

import com.policystrike.api.dto.ApprovePolicyVersionRequest;
import com.policystrike.api.dto.CreatePolicyVersionRequest;
import com.policystrike.api.dto.PolicyVersionResponse;
import com.policystrike.api.dto.RejectPolicyVersionRequest;
import com.policystrike.persistence.service.PolicyVersionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyVersionService policyVersionService;

    public PolicyController(PolicyVersionService policyVersionService) {
        this.policyVersionService = policyVersionService;
    }

    @GetMapping("/{policyId}/versions")
    public List<PolicyVersionResponse> getVersions(@PathVariable String policyId) {
        return policyVersionService.getVersions(policyId);
    }

    @PostMapping("/{policyId}/versions")
    public PolicyVersionResponse createVersion(
            @PathVariable String policyId,
            @RequestBody CreatePolicyVersionRequest request) {
        return policyVersionService.createNextVersion(policyId, request);
    }

    @PostMapping("/{policyId}/versions/{version}/approve")
    public PolicyVersionResponse approveVersion(
            @PathVariable String policyId,
            @PathVariable int version,
            @RequestBody ApprovePolicyVersionRequest request) {
        return policyVersionService.approveVersion(
                policyId, version, request == null ? null : request.approvedBy());
    }

    @PostMapping("/{policyId}/versions/{version}/reject")
    public PolicyVersionResponse rejectVersion(
            @PathVariable String policyId,
            @PathVariable int version,
            @RequestBody RejectPolicyVersionRequest request) {
        return policyVersionService.rejectVersion(
                policyId,
                version,
                request == null ? null : request.rejectedBy(),
                request == null ? null : request.reason());
    }
}
