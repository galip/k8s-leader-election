package com.galip.k8sleaderelection.config;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoordinationV1Api;
import io.kubernetes.client.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesClientConfig {

    @Bean
    public ApiClient apiClient() throws Exception {
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        return client;
    }

    @Bean
    public CoordinationV1Api coordinationV1Api(ApiClient apiClient) {
        return new CoordinationV1Api(apiClient);
    }
}
