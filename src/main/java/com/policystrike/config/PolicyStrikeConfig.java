
package com.policystrike.config;

import com.policystrike.persistence.service.PolicyVersionService;
import com.policystrike.simulation.PolicyStrikeSimulator;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PolicyStrikeConfig {

    @Bean
    public PolicyStrikeSimulator policyStrikeSimulator() {
        return new PolicyStrikeSimulator();
    }

    @Bean
    public CommandLineRunner initializePolicies(
            PolicyVersionService policyVersionService) {

        return args -> policyVersionService.initializeDefaultPolicy();
    }
}
