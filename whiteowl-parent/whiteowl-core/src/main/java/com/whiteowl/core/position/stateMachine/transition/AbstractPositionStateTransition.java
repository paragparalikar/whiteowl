package com.whiteowl.core.position.stateMachine.transition;

import java.util.Collection;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.stateMachine.TradeStateMachine;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractPositionStateTransition implements PositionStateTransition {
	
	private final TradeStateMachine tradeStateMachine;
	@Getter private final PositionStatus initialStatus;

	@Override
	public void transition(@NonNull final Position position) {
		if(!initialStatus.equals(position.getStatus())) throw new IllegalStateException();
		if(log.isInfoEnabled()) log.info("Position transition triggered : {}", position);
		handle(position, position.getExitTrades());
		handle(position, position.getEntryTrades());
	}

	private void handle(Position position, Collection<Trade> trades) {
		trades.stream()
			.filter(trade -> trade.getStatus().isActionable())
			.forEach(trade -> tradeStateMachine.handle(trade, position));
	}

}
