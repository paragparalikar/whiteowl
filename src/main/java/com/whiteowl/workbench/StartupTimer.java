package com.whiteowl.workbench;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public final class StartupTimer {

    private long lastCheckpoint;

    public StartupTimer() {
        this.lastCheckpoint = System.nanoTime();
    }

    public void checkpoint(String label) {
        long now = System.nanoTime();
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(now - lastCheckpoint);
        log.info("Startup [{}] took {} ms", label, elapsedMs);
        lastCheckpoint = now;
    }

}
