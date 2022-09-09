package com.whiteowl.strategy.config;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.whiteowl.core.util.XmlUtils;
import com.whiteowl.strategy.TradingStrategyConfig;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;

@Data
@Entity
@NoArgsConstructor
@Table(name = "TRADING_STRATEGY_CONFIG")
public class PersistentTradingStrategyConfig {

	@Id
	@GeneratedValue
	private Long id;
	
	private boolean enabled;
	
	@Lob
	@Column(nullable = false)
	private String payload;
	
	@NonNull
	@Transient
	private transient TradingStrategyConfig delegate;
	
	public PersistentTradingStrategyConfig(@NonNull TradingStrategyConfig delegate) {
		setDelegate(delegate);
	}
	
	public void setDelegate(@NonNull TradingStrategyConfig delegate) {
		this.delegate = delegate;
		prePersist();
	}
	
	@PreUpdate
	@PrePersist
	public void prePersist() {
		this.id = delegate.getId();
		this.enabled = delegate.isEnabled();
		this.payload = XmlUtils.encode(delegate);
	}
	
	@PostLoad
	public void postLoad() {
		this.delegate = null == payload ? null : XmlUtils.decode(payload);
		if(null != delegate) delegate.setId(id);
	}
	
	@PostPersist
	@SneakyThrows
	public void postPersist() {
		if(null != delegate) delegate.setId(id);
	}
	
}
