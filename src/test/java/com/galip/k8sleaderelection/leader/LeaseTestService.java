package com.galip.k8sleaderelection.leader;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LeaseTestService {

    private static final Logger log = LoggerFactory.getLogger(LeaseTestService.class);
    private final LeaseManager leaseManager;

    public LeaseTestService(LeaseManager leaseManager) {
        this.leaseManager = leaseManager;
    }

    @PostConstruct
    public void testLeaseAccess() {
        try {
            leaseManager.getLease("default", "demo-leader-lease");
            log.info("Lease exists");
        } catch (Exception e) {
            log.warn("Lease not found (this is expected first run)");
        }
    }
}
