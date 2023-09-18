package com.whiteowl.developer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import lombok.SneakyThrows;

public final class BarRepository {
	
	private static final BarRepository INSTANCE = new BarRepository();
	
	public static BarRepository getInstance() {
		return BarRepository.INSTANCE;
	}

	private final DataSource dataSource;
	
	private BarRepository() {
		this.dataSource = createDataSource();
	}
	
	private DataSource createDataSource() {
		final JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUser("sa");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:h2:~/.whiteowl/database/bars");
		return dataSource;
	}
	
	@SneakyThrows
	public void load(final Series series, final int limit, final List<Bar> bars) {
		final String sql = "SELECT * FROM BAR WHERE CODE = ? AND TIMEFRAME = ? ORDER BY CODE ASC, TIMEFRAME ASC, BEGIN_TIME DESC LIMIT ?";
		try(final Connection connection = dataSource.getConnection();
			final PreparedStatement ps = connection.prepareStatement(sql)){
			ps.setString(1, series.code);
			ps.setString(2, series.timeframe.name());
			ps.setInt(3, limit);
			int index = limit;
			try(final ResultSet rs = ps.executeQuery()){
				while(rs.next()) bars.set(--index, new Bar(rs.getFloat("OPEN"), rs.getFloat("HIGH"), 
						rs.getFloat("LOW"), rs.getFloat("CLOSE"), rs.getLong("VOLUME"),
						rs.getTimestamp("BEGIN_TIME").getTime()));
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
