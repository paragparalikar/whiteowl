package com.whiteowl.strategy.test.mock;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

public class MockTaskScheduler implements TaskScheduler {

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
		throw new UnsupportedOperationException();
	}

}
