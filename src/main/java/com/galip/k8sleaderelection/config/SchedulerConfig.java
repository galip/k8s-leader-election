package com.galip.k8sleaderelection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class SchedulerConfig {

    @Bean(name = "leaderElectionScheduler")
    public TaskScheduler leaderElectionScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("leader-elect-");
        scheduler.initialize();

        return scheduler;
    }

    @Bean(name = "jobScheduler")
    public TaskScheduler jobScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("job-");
        scheduler.initialize();

        return scheduler;
    }
}

