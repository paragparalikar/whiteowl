package com.whiteowl.core.bar;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

public class FileSystemBarRepository implements BarRepository {

	

	@Override
	public List<PersistentBar> findAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersistentBar> findAll(Sort sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersistentBar> findAllById(Iterable<PersistentBarKey> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> List<S> saveAll(Iterable<S> entities) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void flush() {
		// TODO Auto-generated method stub

	}

	@Override
	public <S extends PersistentBar> S saveAndFlush(S entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> List<S> saveAllAndFlush(Iterable<S> entities) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteAllInBatch(Iterable<PersistentBar> entities) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllByIdInBatch(Iterable<PersistentBarKey> ids) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllInBatch() {
		// TODO Auto-generated method stub

	}

	@Override
	public PersistentBar getOne(PersistentBarKey id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PersistentBar getById(PersistentBarKey id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> List<S> findAll(Example<S> example) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> List<S> findAll(Example<S> example, Sort sort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Page<PersistentBar> findAll(Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> S save(S entity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<PersistentBar> findById(PersistentBarKey id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean existsById(PersistentBarKey id) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public long count() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteById(PersistentBarKey id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(PersistentBar entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAllById(Iterable<? extends PersistentBarKey> ids) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll(Iterable<? extends PersistentBar> entities) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll() {
		// TODO Auto-generated method stub

	}

	@Override
	public <S extends PersistentBar> Optional<S> findOne(Example<S> example) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> Page<S> findAll(Example<S> example, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S extends PersistentBar> long count(Example<S> example) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <S extends PersistentBar> boolean exists(Example<S> example) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <S extends PersistentBar, R> R findBy(Example<S> example,
			Function<FetchableFluentQuery<S>, R> queryFunction) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<PersistentBar> findTopByCodeAndTimeframeOrderByTimeframeDesc(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersistentBar> findByCodeAndTimeframe(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersistentBar> findByCodeAndTimeframe(String code, Timeframe timeframe, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersistentBar> findByCodeAndTimeframeAndBeginTimeBefore(String code, Timeframe timeframe,
			ZonedDateTime from, Pageable pageable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<PersistentBar> findByCodeAndTimeframeAndBeginTimeBetween(String code, Timeframe timeframe,
			ZonedDateTime from, ZonedDateTime to) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<ZonedDateTime> findMaxBeginTimeByCodeAndTimeframe(String code, Timeframe timeframe) {
		// TODO Auto-generated method stub
		return null;
	}

}
