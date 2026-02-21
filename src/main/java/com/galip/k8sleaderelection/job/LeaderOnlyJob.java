package com.galip.k8sleaderelection.job;

import com.galip.k8sleaderelection.elector.Fabric8LeaderElector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LeaderOnlyJob {

    private static final Logger log = LoggerFactory.getLogger(LeaderOnlyJob.class);

    private final Fabric8LeaderElector elector;

    public LeaderOnlyJob(Fabric8LeaderElector elector) {
        this.elector = elector;
    }

    @Scheduled(fixedDelay = 5000, scheduler = "jobScheduler")
    public void run() throws InterruptedException {

        if (!elector.isLeader()) {
            log.info("Skipping execution — not leader");
            return;
        }
        log.info("Leader executing scheduled job");
        simulateWork();
        log.info("Leader completed scheduled job");
    }

    private void simulateWork() throws InterruptedException {

        log.info("Processing batch step 1");
        Thread.sleep(1000);

        log.info("Processing batch step 2");
        Thread.sleep(1000);

        log.info("Processing batch step 3");
        Thread.sleep(1000);
    }
}
