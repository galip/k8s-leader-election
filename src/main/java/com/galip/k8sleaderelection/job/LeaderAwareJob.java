package com.galip.k8sleaderelection.job;

import com.galip.k8sleaderelection.listener.LeaderListener;
import org.springframework.stereotype.Component;

@Component
public class LeaderAwareJob implements LeaderListener {

    @Override
    public void onStartLeading() {
        System.out.println("I am leader. Starting scheduled jobs...");
    }

    @Override
    public void onStopLeading() {
        System.out.println("Leadership lost. Stopping jobs...");
    }

    @Override
    public void onNewLeader(String identity) {
        System.out.println("New leader elected: " + identity);
    }
}
