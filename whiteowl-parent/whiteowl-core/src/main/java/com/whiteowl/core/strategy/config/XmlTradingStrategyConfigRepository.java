package com.whiteowl.core.strategy.config;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
	
	private final Set<TradingStrategyConfig> cache = new HashSet<>();
	
	@SneakyThrows
	@Synchronized
	private void load() {
		if(cache.isEmpty()) {
			Files.walkFileTree(Constant.HOME, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if(attrs.isRegularFile() && file.endsWith(POSTFIX)) {
						final TradingStrategyConfig config = (TradingStrategyConfig) XmlUtils.read(file).orElse(null);
						if(null != config) cache.add(config);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
	@Override
	public List<TradingStrategyConfig> findAll() {
		load();
		return new ArrayList<>(cache);
	}
	
	@Override
	@Synchronized
	public TradingStrategyConfig save(@NonNull @Valid TradingStrategyConfig config) {
		final Path path = Constant.HOME.resolve(Paths.get(config.getId()));
		XmlUtils.write(path, config);
		cache.removeIf(oldConfig -> {
			return Objects.equals(oldConfig.getClass(), config.getClass()) &&
					Objects.equals(oldConfig.getScripCode(), config.getScripCode()) &&
					Objects.equals(oldConfig.getTimeframe(), config.getTimeframe());
		});
		cache.add(config);
		return config;
	}

}
