package com.whiteowl.core.position.stateMachine;

import java.util.stream.Stream;

import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;

import org.springframework.beans.factory.annotation.Autowired;

import com.whiteowl.core.position.Position;
import com.whiteowl.core.trade.Trade;
import com.whiteowl.core.trade.TradeStatus;

import lombok.NonNull;

public class PositionEntityListener {
	
	@Autowired private PositionStateMachine positionStateMachine;

	@PostUpdate
	@PostPersist
	public void postPersist(@NonNull final Position position) {
		if(Stream.concat(
				position.getExitTrades().stream(), 
				position.getEntryTrades().stream())
			.map(Trade::getStatus)
			.anyMatch(TradeStatus::isActionable)) {
			positionStateMachine.handle(position);
		}
	}
	
}
