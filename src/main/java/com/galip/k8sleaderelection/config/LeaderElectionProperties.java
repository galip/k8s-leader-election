package com.galip.k8sleaderelection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

    @ConfigurationProperties(prefix = "leader-election")
    public record LeaderElectionProperties(
            String namespace,
            String leaseName,
            int renewIntervalSeconds,
            int leaseDurationSeconds
    ) {}