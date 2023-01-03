package com.whiteowl.core.position.stateMachine.transition;

import java.util.Collection;
import java.util.function.Predicate;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.position.PositionStatus;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.stateMachine.TradeStateMachine;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class AbstractPositionStateTransition implements PositionStateTransition {
	
	private final TradeStateMachine tradeStateMachine;
	@Getter private final PositionStatus initialStatus;

	@Override
	public boolean transition(@NonNull final Position position) {
		if(!initialStatus.equals(position.getStatus())) throw new IllegalStateException();
		final boolean exitTradesModified = handle(position, position.getExitTrades());
		final boolean entryTradesModified = handle(position, position.getEntryTrades());
		return exitTradesModified || entryTradesModified;
	}

	private boolean handle(Position position, Collection<Trade> trades) {
		return trades.stream()
			.filter(trade -> trade.getStatus().isActionable())
			.map(trade -> tradeStateMachine.handle(trade, position))
			.anyMatch(Predicate.isEqual(true));
	}

}
