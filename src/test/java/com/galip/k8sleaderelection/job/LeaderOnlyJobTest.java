package com.galip.k8sleaderelection.job;

import com.galip.k8sleaderelection.elector.Fabric8LeaderElector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaderOnlyJobTest {

    @Mock
    private Fabric8LeaderElector elector;

    private LeaderOnlyJob leaderOnlyJob;

    @BeforeEach
    void setUp() {
        leaderOnlyJob = new LeaderOnlyJob(elector);
    }

    @Test
    void run_shouldSkipExecution_whenNotLeader() throws InterruptedException {
        when(elector.isLeader()).thenReturn(false);

        leaderOnlyJob.run();

        verify(elector).isLeader();
        verifyNoMoreInteractions(elector);
    }

    @Test
    void run_shouldExecuteWork_whenLeader() throws InterruptedException {
        when(elector.isLeader()).thenReturn(true);

        leaderOnlyJob.run();

        verify(elector, atLeastOnce()).isLeader();
    }

    @Test
    void run_shouldCheckLeadershipBeforeExecution() throws InterruptedException {
        when(elector.isLeader()).thenReturn(false);

        leaderOnlyJob.run();

        verify(elector).isLeader();
    }
}
