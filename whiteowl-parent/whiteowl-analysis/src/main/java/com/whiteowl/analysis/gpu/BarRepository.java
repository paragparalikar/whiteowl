package com.whiteowl.analysis.gpu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import com.whiteowl.core.bar.Timeframe;

import lombok.NonNull;
import lombok.SneakyThrows;

public class BarRepository {

	private final DataSource dataSource;
	
	public BarRepository() {
		this.dataSource = createDataSource();
	}
	
	private DataSource createDataSource() {
		final JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUser("sa");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:h2:~/.whiteowl/database/bars");
		return dataSource;
	}
	
	private Bar map(ResultSet rs) throws SQLException {
		return Bar.builder()
				.open(rs.getFloat("OPEN"))
				.high(rs.getFloat("HIGH"))
				.low(rs.getFloat("LOW"))
				.close(rs.getFloat("CLOSE"))
				.volume(rs.getLong("VOLUME"))
				.date(rs.getTimestamp("BEGIN_TIME").getTime())
				.build();
	}
	
	@SneakyThrows
	public List<Bar> findLatestReversed(
			@NonNull final String code, 
			@NonNull final Timeframe timeframe, 
			final int limit, final int offset) {
		final String sql = "SELECT * FROM BAR WHERE CODE = ? AND TIMEFRAME = ? ORDER BY CODE ASC, TIMEFRAME ASC, BEGIN_TIME DESC LIMIT ? OFFSET ?";
		try(final Connection connection = dataSource.getConnection();
			final PreparedStatement ps = connection.prepareStatement(sql)){
			ps.setString(1, code);
			ps.setString(2, timeframe.name());
			ps.setInt(3, limit);
			ps.setInt(4, offset);
			try(final ResultSet rs = ps.executeQuery()){
				final List<Bar> bars = new ArrayList<>();
				while(rs.next()) bars.add(map(rs));
				return bars;
			}
		}
	}

	@SneakyThrows
	public List<String> findAllCodes(){
		final String sql = "SELECT DISTINCT(CODE) FROM BAR ORDER BY CODE ASC";
		try(final Connection connection = dataSource.getConnection();
				final PreparedStatement ps = connection.prepareStatement(sql);
				final ResultSet rs = ps.executeQuery()){
			final List<String> codes = new ArrayList<>();
			while(rs.next()) codes.add(rs.getString(1));
			return codes;
		}
	}
}
