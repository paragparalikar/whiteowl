package com.whiteowl.core.trade.stateMachine;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.stateMachine.transition.TradeStateTransition;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeStateMachine {
	
	private final List<TradeStateTransition> tradeStateTransitions;

	public boolean handle(@NonNull final Trade trade, @NonNull final Position position) {
		if(!trade.getStatus().isActionable()) throw new IllegalStateException();
		if(position.getStatus().isTerminal()) throw new IllegalStateException();
		return tradeStateTransitions.stream()
			.filter(tradeStateTranstion -> Objects.equals(trade.getStatus(), 
					tradeStateTranstion.getInitialStatus()))
			.sorted(OrderComparator.INSTANCE)
			.map(tradeStateTranstion -> { tradeStateTranstion.transition(trade, position); return true; })
			.anyMatch(Predicate.isEqual(true));
	}

}
