package com.whiteowl.strategy.test.mock;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import lombok.NonNull;

public class MockTaskScheduler implements TaskScheduler {

	@Override
	public ScheduledFuture<?> schedule(@NonNull final Runnable task, @NonNull final Trigger trigger) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> schedule(@NonNull final Runnable task, @NonNull final Date startTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(
			@NonNull final Runnable task, 
			@NonNull final Date startTime, 
			final long period) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(@NonNull final Runnable task, final long period) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(
			@NonNull final Runnable task, 
			@NonNull final Date startTime, 
			final long delay) {
		throw new UnsupportedOperationException();
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(@NonNull final Runnable task, final long delay) {
		throw new UnsupportedOperationException();
	}

}
