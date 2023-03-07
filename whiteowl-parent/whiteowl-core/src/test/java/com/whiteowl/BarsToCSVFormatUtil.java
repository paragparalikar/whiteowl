package com.whiteowl;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.h2.Driver;

import com.whiteowl.core.bar.Timeframe;

import lombok.Builder;
import lombok.Value;

public class BarsToCSVFormatUtil {
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
	
	private static List<String> getCodes(Connection connection) throws SQLException{
		try(Statement statement = connection.createStatement()){
			final List<String> codes = new ArrayList<>();
			final String selectCodeSql = "select distinct(code) from bar order by code asc";
			try(final ResultSet rs = statement.executeQuery(selectCodeSql)){
				while(rs.next()) codes.add(rs.getString(1));
			}
			return codes;
		}
	}
	
	private static List<Bar> getBars(String code, Timeframe timeframe, PreparedStatement statement) throws SQLException{
		statement.setString(1, code);
		statement.setString(2, timeframe.name());
		try(final ResultSet rs = statement.executeQuery()){
			final List<Bar> bars = new ArrayList<>();
			while(rs.next()) bars.add(toBar(code, rs));
			return bars;
		}
	}
	
	private static Bar toBar(String code, ResultSet rs) throws SQLException {
		return Bar.builder()
				.code(code)
				.timeframe(rs.getString("TIMEFRAME"))
				.timestamp(rs.getTimestamp("BEGIN_TIME").toLocalDateTime())
				.open(rs.getDouble("OPEN"))
				.high(rs.getDouble("HIGH"))
				.low(rs.getDouble("LOW"))
				.close(rs.getDouble("CLOSE"))
				.volume((long) rs.getDouble("VOLUME"))
				.build();
	}
	
	private static String toCSV(Bar bar) {
		return String.join(",", bar.getCode(), 
				DATE_FORMATTER.format(bar.getTimestamp()),
				TIME_FORMATTER.format(bar.getTimestamp()),
				String.valueOf(bar.getOpen()),
				String.valueOf(bar.getHigh()),
				String.valueOf(bar.getLow()),
				String.valueOf(bar.getClose()),
				String.valueOf(bar.getVolume()));
	}

	public static void main(String[] args) throws Exception {
		Driver.load();
		final Path root = Paths.get("C:/workspaces/amibroker/");
		final String url = "jdbc:h2:~/.whiteowl/database/bars";
		final String selectBarSql = "SELECT * FROM BAR WHERE CODE = ? AND TIMEFRAME = ? ORDER BY CODE ASC, TIMEFRAME ASC, BEGIN_TIME DESC";
		try(final Connection connection = DriverManager.getConnection(url, "sa", "");
				final PreparedStatement preparedStatement = connection.prepareStatement(selectBarSql)){
			final List<String> codes = getCodes(connection);
			System.out.printf("Fetched %d codes\n", codes.size());
			for(String code : codes) {
				for(Timeframe timeframe : Timeframe.values()) {
					if(Timeframe.M1.equals(timeframe)) continue;
					final List<Bar> bars = getBars(code, timeframe, preparedStatement);
					final List<String> lines = bars.stream().map(BarsToCSVFormatUtil::toCSV).collect(Collectors.toList());
					final Path path = root.resolve(code + "-" + timeframe.name() + ".csv");
					System.out.printf("Writing %s\n", path.toAbsolutePath());
					Files.write(path, lines);
				}
			}
		}
	}
	
}

@Value
@Builder
class Bar {
	
	private final String code;
	private final String timeframe;
	private final LocalDateTime timestamp;
	private final long volume;
	private final double open, high, low, close;
	
}
