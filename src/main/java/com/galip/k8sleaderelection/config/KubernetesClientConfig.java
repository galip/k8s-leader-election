package com.galip.k8sleaderelection.config;

import com.galip.k8sleaderelection.K8sLeaderElectionApplication;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig {

    private static final Logger log =
            LoggerFactory.getLogger(KubernetesClientConfig.class);

    @Bean(destroyMethod = "close")
    public KubernetesClient kubernetesClient() {

        Config config = Config.autoConfigure(null);

        log.info("Kubernetes client created. Master url: {}", config.getMasterUrl());

        return new DefaultKubernetesClient(config);
    }
}
