package com.whiteowl.core.backtest.v2.optimization;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents one long-running research job. Thread-safe counters so that
 * parallel task execution can update progress.
 */
@Getter
public final class ResearchJob {

    public enum State {
        CREATED, QUEUED, RUNNING, PAUSED, CANCEL_REQUESTED, CANCELLED, FAILED, COMPLETED
    }

    private final String id = UUID.randomUUID().toString();
    private final Instant createdAt = Instant.now();
    private final AtomicInteger totalTasks = new AtomicInteger();
    private final AtomicInteger completedTasks = new AtomicInteger();
    private final AtomicInteger failedTasks = new AtomicInteger();

    private volatile State state = State.CREATED;
    private volatile String currentPhase = "";
    private volatile String currentTimeframe = "";

    public void setState(State state) {
        this.state = state;
    }

    public void setCurrentPhase(String phase) {
        this.currentPhase = phase;
    }

    public void setCurrentTimeframe(String timeframe) {
        this.currentTimeframe = timeframe;
    }

    public void addTotalTasks(int count) {
        totalTasks.addAndGet(count);
    }

    public void taskCompleted() {
        completedTasks.incrementAndGet();
    }

    public void taskFailed() {
        failedTasks.incrementAndGet();
    }

    public double progress() {
        int total = totalTasks.get();
        return total == 0 ? 0.0 : (double) completedTasks.get() / total;
    }

}
