package com.whiteowl.strategy.config;

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
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.stereotype.Repository;

import com.whiteowl.core.bar.Timeframe;
import com.whiteowl.core.util.Constant;
import com.whiteowl.core.util.XmlUtils;
import com.whiteowl.strategy.TradingStrategyTemplate;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;

@Repository
public class XmlTradingStrategyConfigRepository implements TradingStrategyConfigRepository {
	private static final String POSTFIX = "-config.xml";
	
	private final Map<String, TradingStrategyConfig> cache = new ConcurrentHashMap<>();
	
	@SneakyThrows
	@Synchronized
	private void load() {
		if(cache.isEmpty()) {
			Files.walkFileTree(Constant.HOME, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if(attrs.isRegularFile() && file.endsWith(POSTFIX)) {
						final TradingStrategyConfig config = (TradingStrategyConfig) XmlUtils.read(file).orElse(null);
						cache.put(config.getId(), config);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
	@Override
	public List<TradingStrategyConfig> findAll() {
		load();
		return new ArrayList<>(cache.values());
	}

	@Override
	public Optional<TradingStrategyConfig> findById(@NonNull String id) {
		load();
		return Optional.ofNullable(cache.get(id));
	}
	
	@Override
	@Synchronized
	public TradingStrategyConfig save(@NonNull @Valid TradingStrategyConfig config) {
		final Path path = Constant.HOME.resolve(Paths.get(config.getScripCode(), config.getTimeframe().name(), 
				config.getTradingStrategyTemplate().name()));
		XmlUtils.write(path, config);
		cache.put(config.getId(), config);
		return config;
	}
	
	@Override
	@Synchronized
	public List<TradingStrategyConfig> findByTemplate(
			@NonNull TradingStrategyTemplate template) {
		load();
		return cache.values().stream()
				.filter(config -> template.equals(config.getTradingStrategyTemplate()))
				.collect(Collectors.toList());
	}

	@Override
	@Synchronized
	public List<TradingStrategyConfig> findByScripCodeAndTimeframe(
			@NonNull String scripCode,
			@NonNull Timeframe timeframe) {
		load();
		return cache.values().stream()
				.filter(config -> scripCode.equals(config.getScripCode()))
				.filter(config -> timeframe.equals(config.getTimeframe()))
				.collect(Collectors.toList());
	}

	@Override
	@Synchronized
	public Optional<TradingStrategyConfig> findByScripCodeAndTimeframeAndTemplate(
			@NonNull String scripCode,
			@NonNull Timeframe timeframe, 
			@NonNull TradingStrategyTemplate template) {
		load();
		return findById(String.join("-", scripCode, timeframe.name(), template.name()));
	}

}
