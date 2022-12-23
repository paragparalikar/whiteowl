package com.whiteowl.core.position.stateMachine.transition;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.trade.stateMachine.TradeStateMachine;

import lombok.NonNull;

@Component
public class ClosePositionStateTransition  extends AbstractPositionStateTransition {

	@Autowired
	public ClosePositionStateTransition(@NonNull final TradeStateMachine tradeStateMachine) {
		super(tradeStateMachine, PositionStatus.OPEN);
	}	

}
