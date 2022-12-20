package com.whiteowl.core.util;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class Tuple2<K,V> {

	public static <K,V> Tuple2<K,V> of(K key, V value) {
		return new Tuple2<>(key, value);
	}
	
	private final K key;
	private final V value;
	
}
