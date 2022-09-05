package com.whiteowl.client.kite.ticker;

public interface OnError {

	void onError(String error);
	
    void onError(Exception exception);
    
}
