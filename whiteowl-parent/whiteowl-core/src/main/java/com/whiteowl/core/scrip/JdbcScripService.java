package com.whiteowl.core.scrip;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class JdbcScripService implements ScripService {
	
	public static JdbcScripService instance() {
		final JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUser("sa");
		dataSource.setPassword("");
		dataSource.setUrl("jdbc:h2:~/.whiteowl/database/entities");
		return new JdbcScripService(dataSource);
	}

	private final DataSource dataSource;
	private final Map<String, Scrip> cacheByCode = new HashMap<>();
	private final Map<Index, List<Scrip>> cacheByIndices = new HashMap<>();
	
	@SneakyThrows
	private void load() {
		if(cacheByCode.isEmpty()) {
			try(Connection connection = dataSource.getConnection();
					Statement statement = connection.createStatement();
					ResultSet rsScrips = statement.executeQuery("SELECT * FROM SCRIP");
					ResultSet rsIndices = statement.executeQuery("SELECT * FROM SCRIP_INDICES")){
				final Map<String, Set<Index>> indexMappings = new HashMap<>();
				while(rsIndices.next()) {
					indexMappings.computeIfAbsent(rsIndices.getString("SCRIP_CODE"), 
							key -> new HashSet<>()).add(Index.valueOf(rsIndices.getString("INDICES")));
				}
				while(rsScrips.next()) {
					final Scrip scrip = map(rsScrips);
					cacheByCode.put(scrip.getCode(), scrip);
					final Set<Index> indices = indexMappings.get(scrip.getCode());
					scrip.setIndices(indices);
					indices.forEach(index -> cacheByIndices
							.computeIfAbsent(index, key -> new ArrayList<>()).add(scrip));
				}
			}
		}
	}

	private Scrip map(ResultSet rs) throws SQLException {
		final Date expiryDate = rs.getDate("EXPIRY");
		final LocalDate expiry = LocalDate.ofInstant(expiryDate.toInstant(), ZoneId.systemDefault());
		return Scrip.builder()
				.code(rs.getString("CODE"))
				.name(rs.getString("NAME"))
				.expiry(expiry)
				.strike(rs.getDouble("STRIKE"))
				.tickSize(rs.getDouble("TICK_SIZE"))
				.lotSize(rs.getInt("LOT_SIZE"))
				.segment(Segment.values()[rs.getInt("SEGMENT")])
				.type(ScripType.valueOf(rs.getString("TYPE")))
				.exchange(Exchange.valueOf(rs.getString("EXCHANGE")))
				.underlying(rs.getBoolean("UNDERLYING"))
				.build();
	}
	
	@Override
	public List<Scrip> findAll() {
		load();
		return new ArrayList<>(cacheByCode.values());
	}

	@Override
	public Page<Scrip> findByIndices(Index index, Pageable pageable) {
		throw new UnsupportedOperationException();
	}

	@Override
	public long countByIndices(Index index) {
		load();
		return cacheByIndices.get(index).size();
	}

	@Override
	public List<Scrip> findByIndices(Index index) {
		load();
		return cacheByIndices.get(index);
	}

	@Override
	public List<Scrip> saveAll(List<Scrip> scrips) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Scrip findByCode(String code) {
		load();
		return cacheByCode.get(code);
	}

}
