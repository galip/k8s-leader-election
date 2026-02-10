package com.galip.k8sleaderelection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class K8sLeaderElectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(K8sLeaderElectionApplication.class, args);
	}

}
