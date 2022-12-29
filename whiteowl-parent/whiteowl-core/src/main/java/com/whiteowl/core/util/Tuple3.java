package com.whiteowl.core.util;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class Tuple3<T, V, K> {
	
	public static <T, V, K> Tuple3<T, V, K> of(T one, V two, K three){
		return new Tuple3<>(one, two, three);
	}

	private final T one;
	private final V two;
	private final K three;
	
}
