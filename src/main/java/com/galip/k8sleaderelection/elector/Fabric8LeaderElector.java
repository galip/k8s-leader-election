package com.galip.k8sleaderelection.elector;

import com.galip.k8sleaderelection.config.LeaderElectionProperties;
import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Fabric8LeaderElector {

    private static final int GRACE_SECONDS = 2;

    private static final Logger log = LoggerFactory.getLogger(Fabric8LeaderElector.class);

    private final KubernetesClient client;
    private final LeaderElectionProperties properties;

    private final String identity = UUID.randomUUID().toString();

    private final AtomicBoolean leader = new AtomicBoolean(false);

    private volatile String currentLeaderIdentity = null;

    private final AtomicBoolean electionRunning = new AtomicBoolean(false);

    public Fabric8LeaderElector(KubernetesClient client,
                                LeaderElectionProperties properties) {
        this.client = client;
        this.properties = properties;

        log.info("Leader elector started. Identity={}", identity);
    }

    @Scheduled(fixedDelayString = "${leader-election.renew-interval-seconds}000",
               scheduler = "leaderElectionScheduler")
    public void elect() {

        log.info("Leader election cycle started...");

        try {

            if (!electionRunning.compareAndSet(false, true)) {
                log.info("Election already running, skipping");
                return;
            }

            Lease lease = client.leases()
                    .inNamespace(properties.getNamespace())
                    .withName(properties.getLeaseName())
                    .get();

            if (lease == null) {
                log.warn("Lease not found -> {} / {}", properties.getNamespace(), properties.getLeaseName());
                transitionToFollower(null);
                return;
            }

            sanitize(lease);

            String holderIdentity = lease.getSpec().getHolderIdentity();

            boolean expired = isExpired(lease);

            if (holderIdentity == null || holderIdentity.isBlank() || expired) {

                boolean success = attemptTakeover(lease);

                if (success) {
                    transitionToLeader();
                } else {

                    Lease latestLease = client.leases()
                                    .inNamespace(properties.getNamespace())
                                            .withName(properties.getLeaseName())
                                                    .get();

                    String latestHolder = latestLease != null && latestLease.getSpec() != null
                            ? latestLease.getSpec().getHolderIdentity() : null;

                    transitionToFollower(latestHolder);
                }

                return;
            }

            if (identity.equals(holderIdentity)) {

                renew(lease);
                transitionToLeader();
                return;
            }

            transitionToFollower(holderIdentity);

        } catch (KubernetesClientException e) {

            log.warn("Leader election conflict (expected race). {}", e.getMessage());

        } catch (Exception e) {

            log.error("Leader election error", e);
            transitionToFollower(null);
        } finally {
            electionRunning.set(false);
        }
    }

    private boolean attemptTakeover(Lease lease) {

        try {
            String resourceVersion = lease.getMetadata().getResourceVersion();

            Lease updated = new LeaseBuilder(lease)
                    .editMetadata()
                    .withResourceVersion(resourceVersion)
                    .endMetadata()
                    .editSpec()
                    .withHolderIdentity(identity)
                    .withAcquireTime(now())
                    .withRenewTime(now())
                    .endSpec()
                    .build();

            sanitize(updated);

            client.resource(updated).replace();
            log.info("Successfully acquired leadership -> {}", identity);

            return true;

        } catch (KubernetesClientException e) {

            log.debug("Takeover failed due to conflict");
            return false;
        }
    }

    private void renew(Lease lease) {

        String resourceVersion = lease.getMetadata().getResourceVersion();

        Lease updated = new LeaseBuilder(lease)
                .editMetadata()
                .withResourceVersion(resourceVersion)
                .endMetadata()
                .editSpec()
                .withRenewTime(now())
                .endSpec()
                .build();

        sanitize(updated);

        client.resource(updated).replace();
        log.info("Lease renewed by {}", identity);
    }

    private void transitionToLeader() {

        if (leader.compareAndSet(false, true)) {

            currentLeaderIdentity = identity;

            log.info("I AM LEADER -> {}", identity);
        }
    }

    private void transitionToFollower(String newLeader) {

        if (!Objects.equals(currentLeaderIdentity, newLeader)) {

            currentLeaderIdentity = newLeader;

            if (newLeader != null) {
                log.info("Current leader -> {}", newLeader);
            } else {
                log.info("Leader unknown");
            }
        }

        if (leader.compareAndSet(true, false)) {
            log.info("Leadership lost. Previous leader={}", identity);
        }
    }

    private boolean isExpired(Lease lease) {

        if (lease == null || lease.getSpec() == null) {
            return true;
        }

        ZonedDateTime renewTime = lease.getSpec().getRenewTime();
        Integer duration = lease.getSpec().getLeaseDurationSeconds();

        if (renewTime == null || duration == null) {
            return true;
        }

        ZonedDateTime expiryTime = renewTime
                .plusSeconds(duration)
                .plusSeconds(GRACE_SECONDS);


        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        boolean expired = now.isAfter(expiryTime);
        log.info("Lease timing -> renew={}, duration={}, expired={}, now={}",
                renewTime,
                duration,
                expired,
                now);

        if (expired) {
            log.info("Lease expired. renewTime={}, duration={}s, now={}",
                    renewTime, duration, now);
        }

        return expired;
    }


    private ZonedDateTime now() {
        return ZonedDateTime.now(ZoneOffset.UTC);
    }

    private void sanitize(Lease lease) {

        if (lease != null && lease.getMetadata() != null) {

            lease.getMetadata().setManagedFields(null);
            lease.getMetadata().setAnnotations(null);
        }
    }

    public boolean isLeader() {
        return leader.get();
    }
}
