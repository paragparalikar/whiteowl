package com.whiteowl.core.position.event;

import com.whiteowl.core.position.Position;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class PositionSynchronizedEvent {

	@NonNull private final Position position;
	
}
