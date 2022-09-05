package com.whiteowl.core.bar;

import java.io.Serializable;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersistentBarKey implements Serializable {
	private static final long serialVersionUID = 4788174740823367892L;

	private String code;
	private Timeframe timeframe;
	private ZonedDateTime beginTime;
	
}
