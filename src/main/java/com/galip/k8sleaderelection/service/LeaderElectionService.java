package com.galip.k8sleaderelection.service;

import com.galip.k8sleaderelection.leader.LeaseManager;
import io.kubernetes.client.openapi.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class LeaderElectionService {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionService.class);

    private static final String NAMESPACE = "default";
    private static final String LEASE_NAME = "order-processor-leader";
    private static final int LEASE_DURATION_SECONDS = 15;

    private final LeaseManager leaseManager;
    private final String identity = UUID.randomUUID().toString();

    public LeaderElectionService(LeaseManager leaseManager) {
        this.leaseManager = leaseManager;
        log.info("Instance identity: {}", identity);
    }

    @Scheduled(fixedDelay = 2000)
    public void electLeader() {

        try {

            OffsetDateTime now = OffsetDateTime.now();
            V1Lease lease;

            try {
                lease = leaseManager.getLease(NAMESPACE, LEASE_NAME);
            } catch (Exception e) {
                log.info("Lease not found. Creating and becoming leader.");

                V1Lease newLease = new V1Lease()
                        .metadata(new V1ObjectMeta()
                                .name(LEASE_NAME)
                                .namespace(NAMESPACE))
                        .spec(new V1LeaseSpec()
                                .holderIdentity(identity)
                                .leaseDurationSeconds(LEASE_DURATION_SECONDS)
                                .renewTime(now)
                                .acquireTime(now));

                leaseManager.createLease(NAMESPACE, newLease);

                log.info("Leadership acquired (created lease)");
                return;
            }

            V1LeaseSpec spec = lease.getSpec();

            String currentHolder = spec.getHolderIdentity();
            OffsetDateTime renewTime = spec.getRenewTime();
            Integer duration = spec.getLeaseDurationSeconds();

            boolean expired = renewTime == null ||
                    renewTime.plusSeconds(duration).isBefore(now);

            if (identity.equals(currentHolder)) {

                // Renew
                spec.setRenewTime(now);
                leaseManager.replaceLease(LEASE_NAME, NAMESPACE, lease);

                log.info("Leadership renewed by {}", identity);

            } else if (expired) {

                // Takeover
                spec.setHolderIdentity(identity);
                spec.setRenewTime(now);
                spec.setLeaseDurationSeconds(LEASE_DURATION_SECONDS);

                leaseManager.replaceLease(LEASE_NAME, NAMESPACE, lease);

                log.info("Lease expired. Leadership taken by {}", identity);

            } else {
                log.info("Another leader exists: {}", currentHolder);
            }

        } catch (Exception e) {
            log.error("Leader election error", e);
        }
    }
}
