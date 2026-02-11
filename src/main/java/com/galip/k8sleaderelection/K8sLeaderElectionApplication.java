package com.galip.k8sleaderelection;

import com.galip.k8sleaderelection.config.LeaderElectionProperties;
import com.galip.k8sleaderelection.leader.LeaseManager;
import io.kubernetes.client.openapi.models.V1Lease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class K8sLeaderElectionApplication implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(K8sLeaderElectionApplication.class);

	private final LeaseManager leaseManager;
	private final LeaderElectionProperties properties;

	public K8sLeaderElectionApplication(LeaseManager leaseManager,
										LeaderElectionProperties properties) {
		this.leaseManager = leaseManager;
		this.properties = properties;
	}

	public static void main(String[] args) {
		SpringApplication.run(K8sLeaderElectionApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("K8s Leader Election app started");
		log.info("Checking Lease access → namespace='{}' lease='{}'",
				properties.getNamespace(),
				properties.getLeaseName());

		try {
			V1Lease lease = leaseManager.getLease(
					properties.getNamespace(),
					properties.getLeaseName()
			);

			log.info("Lease FOUND. Current holder: {}",
					lease.getSpec() != null ? lease.getSpec().getHolderIdentity() : "none");

		} catch (Exception e) {
			log.warn("Lease not found yet. This is normal on first startup. Message: {}",
					e.getMessage());
		}
	}
}
