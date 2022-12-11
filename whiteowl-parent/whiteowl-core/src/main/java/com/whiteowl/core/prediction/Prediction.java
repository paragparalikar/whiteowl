package com.whiteowl.core.prediction;

import java.time.ZonedDateTime;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.whiteowl.core.bar.Timeframe;
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
public class Prediction {

	@Id
	@GeneratedValue
	private Long id;
	
	private String modelId;
	
	@ManyToOne(optional = false)
	private Scrip scrip;
	
	@Enumerated(EnumType.STRING)
	private Timeframe timeframe;
	
	@Column(nullable = false, columnDefinition = "TIMESTAMP")
	private ZonedDateTime createTimestamp;
	
	@Column(nullable = false, columnDefinition = "TIMESTAMP")
	private ZonedDateTime fromTimestamp;
	
	@Column(nullable = false, columnDefinition = "TIMESTAMP")
	private ZonedDateTime toTimestamp;
	
	@Embedded
	@AttributeOverrides({
        @AttributeOverride(name="min", column=@Column(name="MIN_HIGH")),
        @AttributeOverride(name="max", column=@Column(name="MAX_HIGH"))
    })
	private PredictionRange high;
	
	@Embedded
	@AttributeOverrides({
        @AttributeOverride(name="min", column=@Column(name="MIN_LOW")),
        @AttributeOverride(name="max", column=@Column(name="MAX_LOW"))
    })
	private PredictionRange low;
	
	@Embedded
	@AttributeOverrides({
        @AttributeOverride(name="min", column=@Column(name="MIN_CLOSE")),
        @AttributeOverride(name="max", column=@Column(name="MAX_CLOSE"))
    })
	private PredictionRange close;
	
}
