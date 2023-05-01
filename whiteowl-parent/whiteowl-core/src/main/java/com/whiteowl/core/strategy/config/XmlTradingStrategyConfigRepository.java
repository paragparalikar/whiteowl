package com.whiteowl.core.strategy.config;

import java.io.IOException;
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

import javax.validation.Valid;

import org.springframework.stereotype.Repository;

import com.whiteowl.core.util.Constant;
import com.whiteowl.core.util.XmlUtils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;

@Repository
public class XmlTradingStrategyConfigRepository implements TradingStrategyConfigRepository {
	private static final String POSTFIX = "-config.xml";
	private static final Path DIRECTORY = Constant.HOME.resolve(Paths.get("database", "trading-strategy-configs"));
	
	private final Map<String, TradingStrategyConfig> cache = new ConcurrentHashMap<>();
	
	private Path getPath(String configId) {
		return DIRECTORY.resolve(Paths.get(configId + POSTFIX));
	}
	
	@SneakyThrows
	@Synchronized
	private void load() {
		if(cache.isEmpty() && Files.exists(DIRECTORY)) {
			Files.walkFileTree(DIRECTORY, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if(attrs.isRegularFile() && file.endsWith(POSTFIX)) {
						final TradingStrategyConfig config = (TradingStrategyConfig) XmlUtils.read(file).orElse(null);
						if(null != config) cache.put(config.getId(), config);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
	public TradingStrategyConfig findById(String id) {
		load();
		return cache.get(id);
	}
	
	@Override
	public List<TradingStrategyConfig> findAll() {
		load();
		return new ArrayList<>(cache.values());
	}
	
	@Override
	@SneakyThrows
	@Synchronized
	public TradingStrategyConfig save(@NonNull @Valid TradingStrategyConfig config) {
		Files.createDirectories(DIRECTORY);
		final Path path = getPath(config.getId());
		XmlUtils.write(path, config);
		cache.put(config.getId(), config);
		return config;
	}

}
