package com.whiteowl.strategy.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Repository;

import com.whiteowl.core.util.Constant;
import com.whiteowl.core.util.XmlUtils;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Synchronized;

@Repository
public class XmlTradingStrategyConfigRepository implements TradingStrategyConfigRepository {
	private static final String POSTFIX = "-config.xml";
	
	private Path getPath() {
		return Constant.HOME.resolve(Paths.get("common","configs"));
	}
	
	private Path getPath(String id) {
		return getPath().resolve(id + POSTFIX);
	}
	
	@Override
	@SneakyThrows
	@Synchronized
	public long count() {
		final Path path = getPath();
		if(!Files.exists(path)) return 0l;
		else try(Stream<Path> files = Files.list(path)){
			return files
					.filter(Files::isRegularFile)
					.filter(p -> p.endsWith(POSTFIX))
					.count();
		}
	}

	@Override
	@Synchronized
	public Optional<TradingStrategyConfig> findById(@NonNull String id) {
		return XmlUtils.read(getPath(id));
	}

	@Override
	@Synchronized
	public TradingStrategyConfig save(@NonNull TradingStrategyConfig config) {
		XmlUtils.write(getPath(config.getId()), config);
		return config;
	}

	@Override
	@SneakyThrows
	@Synchronized
	public List<TradingStrategyConfig> findByEnabled(boolean value) {
		final Path path = getPath();
		if(!Files.exists(path)) return Collections.emptyList();
		else try(Stream<Path> files = Files.list(path)){
			return files
					.filter(Files::isRegularFile)
					.filter(p -> p.endsWith(POSTFIX))
					.<Optional<TradingStrategyConfig>>map(XmlUtils::read)
					.<TradingStrategyConfig>map(o -> o.orElse(null))
					.filter(Objects::nonNull)
					.filter(TradingStrategyConfig::isEnabled)
					.sorted(Comparator.comparing(TradingStrategyConfig::getId))
					.collect(Collectors.toList());
		}
	}

}
