package com.whiteowl.core.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
public class ConcurrentExpiryMap<K, V> implements Map<K, V> {

	@NonNull @Setter private Long ttlInMillis = 0L;
	private final Map<K,V> delegate = new ConcurrentHashMap<>();
	private final Map<K, Long> timestamps = new ConcurrentHashMap<>();
	
	private void cleanUp() {
		final Long now = System.currentTimeMillis();
		final Iterator<Entry<K, Long>> iterator = timestamps.entrySet().iterator();
		while(iterator.hasNext()) {
			final Entry<K, Long> entry = iterator.next();
			if(now - entry.getValue() > ttlInMillis) {
				iterator.remove();
				delegate.remove(entry.getKey());
			}
		}
	}

	@Override
	public int size() {
		cleanUp();
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		cleanUp();
		return delegate.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		cleanUp();
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		cleanUp();
		return delegate.containsValue(value);
	}

	@Override
	public V get(Object key) {
		cleanUp();
		return delegate.get(key);
	}

	@Override
	public V put(K key, V value) {
		timestamps.put(key, System.currentTimeMillis());
		return delegate.put(key, value);
	}

	@Override
	public V remove(Object key) {
		timestamps.remove(key);
		return delegate.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		final Long now = System.currentTimeMillis();
		m.keySet().forEach(key -> timestamps.put(key, now));
		delegate.putAll(m);
	}

	@Override
	public void clear() {
		timestamps.clear();
		delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		cleanUp();
		return delegate.keySet();
	}

	@Override
	public Collection<V> values() {
		cleanUp();
		return delegate.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		cleanUp();
		return delegate.entrySet();
	}

}
