package com.galip.k8sleaderelection.leader;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoordinationV1Api;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Config;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class LeaseManager {

    private static final String NAMESPACE = "default";
    private static final String LEASE_NAME = "order-processor-leader";
    private static final int LEASE_DURATION_SECONDS = 15;

    private final String identity = UUID.randomUUID().toString();
    private CoordinationV1Api api;

    @PostConstruct
    public void start() throws Exception {
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        api = new CoordinationV1Api();

        System.out.println("Instance identity: " + identity);

        new Thread(this::leaderLoop).start();
    }

    private void leaderLoop() {
        while (true) {
            try {
                acquireOrRenew();
                Thread.sleep(5000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public V1Lease getLease(String namespace, String leaseName) throws Exception {
        return api.readNamespacedLease(leaseName, namespace).execute();
    }

    private void acquireOrRenew() throws Exception {

        OffsetDateTime now = OffsetDateTime.now();
        V1Lease lease;

        try {
            lease = api.readNamespacedLease(LEASE_NAME, NAMESPACE).execute();

        } catch (Exception e) {
            System.out.println("Lease not found, creating...");
            createLease(now);
            return;
        }

        V1LeaseSpec spec = lease.getSpec();
        String currentHolder = spec.getHolderIdentity();
        OffsetDateTime renewTime = spec.getRenewTime();
        Integer duration = spec.getLeaseDurationSeconds();

        boolean expired = renewTime == null ||
                renewTime.plusSeconds(duration).isBefore(now);

        if (expired || identity.equals(currentHolder)) {
            System.out.println("Acquiring or renewing leadership...");
            updateLease(lease, now);
        } else {
            System.out.println("Another leader exists: " + currentHolder);
        }
    }

    public void createLease(String namespace, V1Lease lease) throws Exception {
        api.createNamespacedLease(namespace, lease).execute();
    }

    public void replaceLease(String leaseName, String namespace, V1Lease lease) throws Exception {
        api.replaceNamespacedLease(leaseName, namespace, lease).execute();
    }

    private void createLease(OffsetDateTime now) throws Exception {

        V1Lease lease = new V1Lease()
                .metadata(new V1ObjectMeta()
                        .name(LEASE_NAME)
                        .namespace(NAMESPACE))
                .spec(new V1LeaseSpec()
                        .holderIdentity(identity)
                        .leaseDurationSeconds(LEASE_DURATION_SECONDS)
                        .renewTime(now)
                        .acquireTime(now));

        api.createNamespacedLease(NAMESPACE, lease).execute();

        System.out.println("Leadership acquired (created lease)");
    }

    private void updateLease(V1Lease lease, OffsetDateTime now) throws Exception {

        V1LeaseSpec spec = lease.getSpec();
        spec.setHolderIdentity(identity);   // kendimiz leader oluyoruz
        spec.setRenewTime(now);
        spec.setLeaseDurationSeconds(LEASE_DURATION_SECONDS);

        api.replaceNamespacedLease(
                lease.getMetadata().getName(),
                lease.getMetadata().getNamespace(),
                lease
        ).execute();

        System.out.println("Leadership renewed by: " + identity);
    }
}
