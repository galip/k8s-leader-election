package com.galip.k8sleaderelection.elector;

import com.galip.k8sleaderelection.listener.LeaderListener;
import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;


import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class Fabric8LeaderElector {

    private final KubernetesClient client;
    private final LeaderListener listener;

    private final String namespace = "default";
    private final String leaseName = "app-leader";
    private final String identity = UUID.randomUUID().toString();

    private final int leaseDurationSeconds = 15;
    private final int renewIntervalSeconds = 5;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private final AtomicBoolean leading = new AtomicBoolean(false);

    public Fabric8LeaderElector(
            KubernetesClient client,
            LeaderListener listener
    ) {
        this.client = client;
        this.listener = listener;
    }

    @PostConstruct
    public void start() {
        scheduler.scheduleWithFixedDelay(
                this::elect,
                0,
                renewIntervalSeconds,
                TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        if (leading.get()) {
            listener.onStopLeading();
        }
    }

    private void elect() {
        try {

            Lease lease = client.leases()
                    .inNamespace(namespace)
                    .withName(leaseName)
                    .get();

            if (lease == null) {
                createLease();
                return;
            }

            String holder = lease.getSpec().getHolderIdentity();

            if (isExpired(lease)) {
                takeOver(lease);
            } else if (identity.equals(holder)) {
                renew(lease);
            } else {
                transitionToFollower(holder);
            }

        } catch (Exception e) {
            transitionToFollower(null);
        }
    }

    private void createLease() {

        Lease lease = new LeaseBuilder()
                .withNewMetadata()
                .withName(leaseName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withHolderIdentity(identity)
                .withLeaseDurationSeconds(leaseDurationSeconds)
                .withAcquireTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                .endSpec()
                .build();

        client.resource(lease).create();
        transitionToLeader();
    }

    private void takeOver(Lease lease) {

        Lease updated = new LeaseBuilder(lease)
                .editSpec()
                .withHolderIdentity(identity)
                .withAcquireTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                .endSpec()
                .build();

        client.resource(updated)
                .lockResourceVersion(lease.getMetadata().getResourceVersion())
                .replace();

        transitionToLeader();
    }

    private void renew(Lease lease) {

        Lease updated = new LeaseBuilder(lease)
                .editSpec()
                .withRenewTime(ZonedDateTime.now(ZoneOffset.UTC))
                .endSpec()
                .build();

        client.resource(updated)
                .lockResourceVersion(lease.getMetadata().getResourceVersion())
                .replace();
    }

    private boolean isExpired(Lease lease) {

        ZonedDateTime renewTime = lease.getSpec().getRenewTime();
        Integer duration = lease.getSpec().getLeaseDurationSeconds();

        if (renewTime == null || duration == null) {
            return true;
        }

        ZonedDateTime expiry = renewTime.plusSeconds(duration);

        return ZonedDateTime.now(ZoneOffset.UTC).isAfter(expiry);
    }

    private void transitionToLeader() {
        if (leading.compareAndSet(false, true)) {
            listener.onStartLeading();
        }
    }

    private void transitionToFollower(String currentLeader) {
        if (leading.compareAndSet(true, false)) {
            listener.onStopLeading();
        }
        if (currentLeader != null) {
            listener.onNewLeader(currentLeader);
        }
    }

    public boolean isLeader() {
        return leading.get();
    }
}

