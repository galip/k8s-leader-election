package com.galip.k8sleaderelection.leader;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoordinationV1Api;
import io.kubernetes.client.openapi.models.V1Lease;
import org.springframework.stereotype.Component;

@Component
public class LeaseManager {

    private final CoordinationV1Api api;

    public LeaseManager(ApiClient client) {
        this.api = new CoordinationV1Api(client);
    }

    public V1Lease getLease(String namespace, String name) throws Exception {
        return api.readNamespacedLease(name, namespace).execute();
    }
}
