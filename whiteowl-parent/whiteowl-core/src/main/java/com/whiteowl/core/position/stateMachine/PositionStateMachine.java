package com.whiteowl.core.position.stateMachine;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionService;
import com.whiteowl.core.position.stateMachine.transition.PositionStateTransition;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PositionStateMachine {

	private final PositionService positionService;
	private final List<PositionStateTransition> positionStateTransitions;
	
	public void handle(@NonNull final Position position) {
		positionStateTransitions.stream()
			.filter(positionStateTransition -> Objects.equals(position.getStatus(), 
					positionStateTransition.getInitialStatus()))
			.forEach(positionStateTransition -> positionStateTransition.transition(position));
		position.updateStatus();
		positionService.save(position);
	}
	
}
