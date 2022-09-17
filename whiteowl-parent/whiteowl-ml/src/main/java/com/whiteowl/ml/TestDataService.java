package com.whiteowl.ml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.num.DoubleNum;

import lombok.SneakyThrows;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

public class TestDataService {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final Path inputDirectory = Paths.get("C:\\ml\\data\\input-small");
	private static final Path outputDirectory = Paths.get("C:\\ml\\data\\output");
	
	@SneakyThrows
	public Stream<Path> getAllFiles(){
		return Files.list(inputDirectory)
				.filter(path -> !path.getFileName().toString().contains("NIFTY"))
				.filter(path -> {
					try {
						return Files.size(path) > 240 * 1024;
					} catch (IOException e) {
						e.printStackTrace();
						return false;
					}
				});
	}
	
	public void splitAndSave(int folds, String name, Instances instances) throws IOException {
		instances.stratify(folds);
		save(name + "-full.arff", instances);
		save(name + "-train.arff", instances.trainCV(folds, 1));
		save(name + "-test.arff", instances.testCV(folds, 1));
	}
	
	public void save(String fileName, Instances instances) throws IOException {
		final ArffSaver arffSaver = new ArffSaver();
		arffSaver.setInstances(instances);
		arffSaver.setFile(outputDirectory.resolve(fileName).toFile());
		arffSaver.writeBatch();
	}

	@SneakyThrows
	public BarSeries getBarSeries(Path path) {
		final List<Bar> bars = Files.lines(path).map(this::parse).distinct()
				.sorted(Comparator.comparing(Bar::getEndTime)).collect(Collectors.toList());
		return new BaseBarSeries(bars);
	}
	
	private Bar parse(String line) {
		final String[] tokens = line.split(",");
		return BaseBar.builder().timePeriod(Duration.ofDays(1))
				.endTime(LocalDate.parse(tokens[1], formatter).atStartOfDay(ZoneId.systemDefault()))
				.openPrice(DoubleNum.valueOf(tokens[2])).highPrice(DoubleNum.valueOf(tokens[3]))
				.lowPrice(DoubleNum.valueOf(tokens[4])).closePrice(DoubleNum.valueOf(tokens[5]))
				.volume(DoubleNum.valueOf(tokens[6])).build();
	}

}
