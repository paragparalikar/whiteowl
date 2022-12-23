package com.whiteowl.core.position.stateMachine.transition;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;

public interface PositionStateTransition {
	
	PositionStatus getInitialStatus();
	
	void transition(Position position);

}
