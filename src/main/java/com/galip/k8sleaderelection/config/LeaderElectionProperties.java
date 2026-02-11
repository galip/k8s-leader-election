package com.galip.k8sleaderelection.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "leader-election")
public class LeaderElectionProperties {

    private String namespace;
    private String leaseName;
    private int leaseDurationSeconds;
    private int renewIntervalSeconds;

    public String getNamespace() { return namespace; }
    public void setNamespace(String namespace) { this.namespace = namespace; }

    public String getLeaseName() { return leaseName; }
    public void setLeaseName(String leaseName) { this.leaseName = leaseName; }

    public int getLeaseDurationSeconds() { return leaseDurationSeconds; }
    public void setLeaseDurationSeconds(int leaseDurationSeconds) { this.leaseDurationSeconds = leaseDurationSeconds; }

    public int getRenewIntervalSeconds() { return renewIntervalSeconds; }
    public void setRenewIntervalSeconds(int renewIntervalSeconds) { this.renewIntervalSeconds = renewIntervalSeconds; }
}
