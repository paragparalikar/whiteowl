package com.whiteowl.developer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

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
	
	@SneakyThrows
	public List<Bar> load(
			@NonNull final String code, 
			@NonNull final String timeframe, 
			final int limit, final int offset) {
		final String sql = "SELECT * FROM BAR WHERE CODE = ? AND TIMEFRAME = ? ORDER BY CODE ASC, TIMEFRAME ASC, BEGIN_TIME DESC LIMIT ? OFFSET ?";
		try(final Connection connection = dataSource.getConnection();
			final PreparedStatement ps = connection.prepareStatement(sql)){
			ps.setString(1, code);
			ps.setString(2, timeframe);
			ps.setInt(3, limit);
			ps.setInt(4, offset);
			try(final ResultSet rs = ps.executeQuery()){
				final List<Bar> bars = new ArrayList<>();
				while(rs.next()) bars.add(new Bar(rs.getFloat("OPEN"), rs.getFloat("HIGH"), 
						rs.getFloat("LOW"), rs.getFloat("CLOSE"), rs.getLong("VOLUME"),
						rs.getTimestamp("BEGIN_TIME").getTime()));
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
