package com.whiteowl.ui.vaadin.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.whiteowl.core.BaseEvent;

import lombok.NonNull;

@Component
public class Broadcaster {
	
	@SuppressWarnings("rawtypes")
	private static final Map<Class, List<Consumer>> cache = new HashMap<>();
    private static final Executor executor = Executors.newSingleThreadExecutor();

    public static synchronized <T> void register(Class<T> type, Consumer<T> callback) {
    	cache.computeIfAbsent(type, key -> new LinkedList<>()).add(callback);
    }
    
    public static synchronized <T> void deregister(Class<T> type, Consumer<T> callback) {
    	cache.computeIfAbsent(type, key -> new LinkedList<>()).remove(callback);
    }
    
    @SuppressWarnings("unchecked")
	public static synchronized <T> void broadcast(@NonNull T event) {
    	for(Consumer<T> callback : cache.computeIfAbsent(event.getClass(), key -> new LinkedList<>())) {
    		executor.execute(() -> callback.accept(event));
    	}
    }
    
    @EventListener
	private void onMongooseEvent(BaseEvent event) {
		Broadcaster.broadcast(event);
	}
}