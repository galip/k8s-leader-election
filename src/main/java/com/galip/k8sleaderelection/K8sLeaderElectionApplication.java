package com.galip.k8sleaderelection;

import com.galip.k8sleaderelection.config.LeaderElectionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class K8sLeaderElectionApplication {

	private static final Logger log =
			LoggerFactory.getLogger(K8sLeaderElectionApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(K8sLeaderElectionApplication.class, args);
		log.info("K8s Leader Election application started successfully.");
	}
}
