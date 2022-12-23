package com.whiteowl.core.trade.stateMachine.transition;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

public interface TradeStateTransition {

	TradeStatus getInitialStatus();
	
	Trade transition(Trade trade, Position position);
	
}
