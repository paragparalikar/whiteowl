package com.whiteowl.core.trade.stateMachine;

import org.springframework.stereotype.Component;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TradeStateMachine {

	public void handle(@NonNull final Trade trade, @NonNull final Position position) {
		if(!trade.getStatus().isActionable()) throw new IllegalStateException();
		if(position.getStatus().isTerminal()) throw new IllegalStateException();
		
	}

}
