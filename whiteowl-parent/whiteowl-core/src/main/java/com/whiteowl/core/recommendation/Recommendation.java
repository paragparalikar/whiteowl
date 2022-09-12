package com.whiteowl.core.recommendation;

import java.time.ZonedDateTime;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.ta4j.core.Trade.TradeType;

import com.whiteowl.core.prediction.Prediction;
import com.whiteowl.core.scrip.Scrip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

	@Id
	@GeneratedValue
	private Long id;
	
	@ManyToOne(optional = true)
	private Prediction prediction;
	
	// Prediction could of NIFTY, this scrip will be actual instrument to be traded like CE
	@ManyToOne(optional = false)
	private Scrip scrip; 

	@Enumerated(EnumType.STRING)
	private TradeType tradeType;
	
	// Will be present only when more than one scrips 
	// are to be traded as part of single position like in iron condor
	// All recommendations with same correlation id form a single position
	private String correlationId; 
	
	private Double entryPrice;
	
	private Double targetPrice;
	
	private Double stopLossPrice;
	
	private Integer timeStopBarCount;
	
	private ZonedDateTime effectiveTimestamp;
	
	private ZonedDateTime expiryTimestamp;
}


