package com.galip.k8sleaderelection.elector;

import com.galip.k8sleaderelection.config.LeaderElectionProperties;
import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;

@Component
public class Fabric8LeaderElector {

    private final KubernetesClient client;
    private final LeaderElectionProperties properties;

    private final String identity = UUID.randomUUID().toString();

    private volatile boolean isLeader = false;
    private volatile String currentLeader = null;

    public Fabric8LeaderElector(KubernetesClient client,
                                LeaderElectionProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${leader-election.renew-interval-seconds}000")
    public void elect() {
        try {

            Lease lease = client.leases()
                    .inNamespace(properties.getNamespace())
                    .withName(properties.getLeaseName())
                    .get();

            if (lease == null) {
                return;
            }

            String holderIdentity = lease.getSpec().getHolderIdentity();

            // Leader yoksa veya expired ise takeover dene
            if (holderIdentity == null || isExpired(lease)) {
                takeOver(lease);
                return;
            }

            // Ben leader isem renew et
            if (identity.equals(holderIdentity)) {
                renew(lease);
                return;
            }

            // Follower durumundayım
            transitionToFollower(holderIdentity);

        } catch (Exception e) {
            e.printStackTrace();
            transitionToFollower(null);
        }
    }

    private void takeOver(Lease lease) {

        lease.getMetadata().setManagedFields(null);

        Lease updated = new LeaseBuilder(lease)
                .editSpec()
                .withHolderIdentity(identity)
                .withAcquireTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                .endSpec()
                .build();

        client.resource(updated).replace();

        transitionToLeader();
    }

    private void renew(Lease lease) {

        lease.getMetadata().setManagedFields(null);

        Lease updated = new LeaseBuilder(lease)
                .editSpec()
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                .endSpec()
                .build();

        client.resource(updated).replace();
    }

    private boolean isExpired(Lease lease) {
        ZonedDateTime renewTime = lease.getSpec().getRenewTime();
        Integer duration = lease.getSpec().getLeaseDurationSeconds();

        if (renewTime == null || duration == null) {
            return true;
        }

        return renewTime.plusSeconds(duration)
                .isBefore(ZonedDateTime.now(ZoneOffset.UTC));
    }

    private void transitionToLeader() {
        if (!isLeader) {
            isLeader = true;
            currentLeader = identity;
            System.out.println("I am leader. Identity=" + identity + " — Starting scheduled jobs...");
        }
    }

    private void transitionToFollower(String newLeader) {

        // Leader değiştiyse 1 kere log bas
        if (!Objects.equals(currentLeader, newLeader)) {
            currentLeader = newLeader;

            if (newLeader != null) {
                System.out.println("New leader elected: " + newLeader);
            }
        }

        if (isLeader) {
            isLeader = false;
            System.out.println("Leadership lost. Previous leader identity=" + identity + " — Stopping jobs...");
        }
    }
}
