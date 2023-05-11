package com.whiteowl.core.analysis.performance;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.whiteowl.core.util.Constant;

import lombok.SneakyThrows;
import lombok.Synchronized;

@Repository
public class FileSystemTradingStrategyConfigPerformanceRepository implements TradingStrategyConfigPerformanceRepository {
	private static final String POSTFIX = ".txt";
	private static final Path DIRECTORY = Constant.HOME.resolve(Paths.get("database", "trading-strategy-config-performances"));
	
	private final Map<String, TradingStrategyConfigPerformance> cache = new ConcurrentHashMap<>();
	
	private Path getPath(String id) {
		return DIRECTORY.resolve(Paths.get(id + POSTFIX));
	}
	
	@SneakyThrows
	public void save(TradingStrategyConfigPerformance performance) {
		load();
		final Path path = getPath(performance.getId());
		Files.createDirectories(DIRECTORY);
		try(ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))){
			oos.writeObject(performance);
			cache.put(performance.getId(), performance);
		}
	}
	
	@SneakyThrows
	public TradingStrategyConfigPerformance findById(String id) {
		load();
		return cache.computeIfAbsent(id, key -> read(getPath(key)));
	}
	
	public List<TradingStrategyConfigPerformance> findAll(){
		load();
		return new ArrayList<>(cache.values());
	}
	
	@SneakyThrows
	private TradingStrategyConfigPerformance read(Path path) {
		if(!Files.exists(path)) return null;
		try(ObjectInputStream oos = new ObjectInputStream(Files.newInputStream(path))){
			return (TradingStrategyConfigPerformance) oos.readObject();
		}
	}
	
	@SneakyThrows
	@Synchronized
	private void load() {
		if(cache.isEmpty() && Files.exists(DIRECTORY)) {
			Files.walkFileTree(DIRECTORY, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if(attrs.isRegularFile() && file.endsWith(POSTFIX)) {
						final TradingStrategyConfigPerformance performance = read(file);
						if(null != performance) cache.put(performance.getId(), performance);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
}
