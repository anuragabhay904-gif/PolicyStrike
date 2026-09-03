package com.refundshield.recoveryagent;

import com.refundshield.recoveryagent.repository.CustomerRepository;
import com.refundshield.recoveryagent.service.DemoDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RevenueLeakRecoveryAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(RevenueLeakRecoveryAgentApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RevenueLeakRecoveryAgentApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(CustomerRepository customerRepository, DemoDataService demoDataService) {
        return args -> {
            if (customerRepository.count() == 0) {
                logger.info("Empty database detected. Auto-seeding initial demo data...");
                demoDataService.seedDemoData();
                logger.info("Demo data initialized successfully.");
            }
        };
    }
}
