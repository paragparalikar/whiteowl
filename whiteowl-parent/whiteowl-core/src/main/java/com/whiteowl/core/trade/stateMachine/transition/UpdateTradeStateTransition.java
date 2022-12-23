package com.whiteowl.core.trade.stateMachine.transition;

import org.springframework.stereotype.Component;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UpdateTradeStateTransition implements TradeStateTransition {

	@Override
	public TradeStatus getInitialStatus() {
		return TradeStatus.UPDATABLE;
	}

	@Override
	public Trade transition(@NonNull final Trade trade, @NonNull final Position position) {
		if(!getInitialStatus().equals(trade.getStatus())) throw new IllegalStateException();
		return trade;
	}

}
