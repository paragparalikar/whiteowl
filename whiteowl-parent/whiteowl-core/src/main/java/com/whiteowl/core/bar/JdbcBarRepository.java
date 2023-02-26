package com.whiteowl.core.bar;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.stereotype.Repository;
import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.util.Constant;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import lombok.NonNull;
import lombok.SneakyThrows;

@Repository
public class JdbcBarRepository implements BarRepository, AutoCloseable {
	
	private static HikariDataSource createDataSource(final DataSourceProperties properties) {
		final HikariConfig config = new HikariConfig();
		config.setDriverClassName(properties.getDriverClassName());
		config.setJdbcUrl(properties.getUrl().replace("entities", "bars"));
		config.setUsername(properties.getUsername());
		config.setPassword(properties.getPassword());
		config.setAutoCommit(true);
		config.setMinimumIdle(1);
		config.setMaximumPoolSize(1);
		return new HikariDataSource(config);
	}

	
	private final DataSource dataSource;
	
	public JdbcBarRepository(final DataSource dataSource) {
		this.dataSource = dataSource;
		createTableIfNotExists();
	}

	@Autowired
	public JdbcBarRepository(final DataSourceProperties properties) {
		this(JdbcBarRepository.createDataSource(properties));
	}
	
	@Override
	public void close() throws Exception {
		if(dataSource instanceof HikariDataSource) dataSource.unwrap(HikariDataSource.class).close();
	}
	
	@SneakyThrows
	private void createTableIfNotExists() {
		try(final Connection connection = dataSource.getConnection()){
			connection.createStatement().execute("CREATE TABLE IF NOT EXISTS BAR ( "
					+ "CODE VARCHAR(255) NOT NULL, "
					+ "TIMEFRAME VARCHAR(3) NOT NULL,"
					+ "BEGIN_TIME TIMESTAMP WITH TIME ZONE NOT NULL, "
					+ "OPEN DOUBLE PRECISION DEFAULT 0, "
					+ "HIGH DOUBLE PRECISION DEFAULT 0, "
					+ "LOW DOUBLE PRECISION DEFAULT 0, "
					+ "CLOSE DOUBLE PRECISION DEFAULT 0, "
					+ "VOLUME DOUBLE PRECISION DEFAULT 0, "
					+ "PRIMARY KEY (CODE ASC, TIMEFRAME ASC, BEGIN_TIME DESC))");
		}
	}
	
	private Bar map(ResultSet rs) throws SQLException {
		final Timestamp beginTimestamp = rs.getTimestamp("BEGIN_TIME");
		final ZonedDateTime beginTime = beginTimestamp != null ? ZonedDateTime.ofInstant(
		        Instant.ofEpochMilli(beginTimestamp.getTime()), ZoneOffset.UTC) : null;
		final Duration timePeriod = Timeframe.valueOf(rs.getString("TIMEFRAME")).getDuration();
		return BaseBar.builder()
				.openPrice(DoubleNum.valueOf(rs.getDouble("OPEN")))
				.highPrice(DoubleNum.valueOf(rs.getDouble("HIGH")))
				.lowPrice(DoubleNum.valueOf(rs.getDouble("LOW")))
				.closePrice(DoubleNum.valueOf(rs.getDouble("CLOSE")))
				.volume(DoubleNum.valueOf(rs.getDouble("VOLUME")))
				.endTime(beginTime.plus(timePeriod))
				.timePeriod(timePeriod)
				.build();
	}
	
	@Override
	@SneakyThrows
	public List<Bar> findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe, 
			final int count) {
		final String sql = "SELECT * FROM BAR WHERE CODE = ? AND TIMEFRAME = ? ORDER BY CODE ASC, TIMEFRAME ASC, BEGIN_TIME DESC LIMIT ?";
		try(final Connection connection = dataSource.getConnection();
			final PreparedStatement ps = connection.prepareStatement(sql)){
			ps.setString(1, code);
			ps.setString(2, timeframe.name());
			ps.setInt(3, count);
			try(final ResultSet rs = ps.executeQuery()){
				final List<Bar> bars = new ArrayList<>();
				while(rs.next()) bars.add(map(rs));
				Collections.reverse(bars);
				return bars;
			}
		}
	}

	@Override
	@SneakyThrows
	public void saveAll(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe, 
			@NonNull final Collection<Bar> bars) {
		final String sql = "MERGE INTO BAR VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
		try(final Connection connection = dataSource.getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql)){
			for(Bar bar : bars) {
				ps.setString(1, code);
				ps.setString(2, timeframe.name());
				ps.setTimestamp(3, Timestamp.valueOf(bar.getBeginTime().toLocalDateTime()));
				ps.setDouble(4, bar.getOpenPrice().doubleValue());
				ps.setDouble(5, bar.getHighPrice().doubleValue());
				ps.setDouble(6, bar.getLowPrice().doubleValue());
				ps.setDouble(7, bar.getClosePrice().doubleValue());
				ps.setDouble(8, bar.getVolume().doubleValue());
				ps.addBatch();
			}
			ps.executeBatch();
		}
	}
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		final DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername("sa");
		properties.setPassword("");
		properties.setDriverClassName("org.h2.Driver");
		properties.setUrl("jdbc:h2:~/.whiteowl/database/entities;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE");
		try(final JdbcBarRepository jdbcBarRepository = new JdbcBarRepository(properties)){
			final BarRepository fileSystemBarRepository = new FileSystemBarRepository();
			Files.list(Constant.HOME.resolve("backup"))
				.map(Path::getFileName)
				.map(Path::toString)
				.forEach(code -> {
					for(Timeframe timeframe : Timeframe.values()) {
						final List<Bar> bars = fileSystemBarRepository.findLatestByCodeAndTimeframeOrderByBeginTimeAsc(
								code, timeframe, Integer.MAX_VALUE);
						System.out.printf("%-25s %-5s %6d\n", code, timeframe.name(), bars.size());
						jdbcBarRepository.saveAll(code, timeframe, bars);
					}
				});
		}
		
	}

}
