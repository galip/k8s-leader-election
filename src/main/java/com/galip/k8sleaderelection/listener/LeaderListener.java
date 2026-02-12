package com.galip.k8sleaderelection.listener;

public interface LeaderListener {

    void onStartLeading();

    void onStopLeading();

    default void onNewLeader(String identity) {
        // optional
    }
}
