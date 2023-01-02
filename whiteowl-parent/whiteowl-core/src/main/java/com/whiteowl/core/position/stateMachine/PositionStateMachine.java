package com.whiteowl.core.position.stateMachine;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.core.OrderComparator;
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
		if(position.getStatus().isTerminal()) throw new IllegalStateException();
		final boolean positionModified = positionStateTransitions.stream()
			.filter(positionStateTransition -> Objects.equals(position.getStatus(), 
					positionStateTransition.getInitialStatus()))
			.sorted(OrderComparator.INSTANCE)
			.map(positionStateTransition -> positionStateTransition.transition(position))
			.anyMatch(Predicate.isEqual(true));
		if(positionModified) positionService.save(position);
	}
	
}
