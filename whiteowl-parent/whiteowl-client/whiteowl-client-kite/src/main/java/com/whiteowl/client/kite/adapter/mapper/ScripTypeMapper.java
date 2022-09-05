package com.whiteowl.client.kite.adapter.mapper;

import com.whiteowl.client.kite.model.InstrumentType;
import com.whiteowl.core.scrip.ScripType;

public class ScripTypeMapper {

	public ScripType toScripType(InstrumentType instrumentType) {
		if(null == instrumentType) return null;
		switch(instrumentType) {
		case CE:return ScripType.CE;
		case EQ:return ScripType.EQ;
		case FUT:return ScripType.FUT;
		case PE:return ScripType.PE;
		default:return null;
		}
	}
	
}
