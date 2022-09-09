package com.whiteowl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.whiteowl.core.broker.Broker;
import com.whiteowl.core.portfolio.Credentials;
import com.whiteowl.core.portfolio.Portfolio;
import com.whiteowl.core.portfolio.PortfolioService;
import com.whiteowl.core.user.User;
import com.whiteowl.core.user.UserService;
import com.whiteowl.strategy.config.TradingStrategyConfigService;
import com.whiteowl.strategy.shortstrangle.ShortStraddleConfig;

@Profile("dev")
@Configuration
public class DevConfiguration {

	@Bean
	public CommandLineRunner configureUsers(UserService userService) {
		return args -> {
			if(0 == userService.count()) {
				final User user = User.builder()
						.username("paragparalikar")
						.firstName("Parag")
						.lastName("Paralikar")
						.password("Welcome@1")
						.build();
				userService.save(user);
			}
		};
	}
	
	@Bean
	public CommandLineRunner configureTradingStrategyConfigs(TradingStrategyConfigService configService) {
		return args -> { if(0 == configService.count()) configService.save(new ShortStraddleConfig()); };
	}
	
	@Bean
	public CommandLineRunner configurePortfolio(PortfolioService portfolioService) {
		return args -> {
			if(0 == portfolioService.count()) {
				final Portfolio portfolio = new Portfolio();
				final Credentials credentials = Credentials.builder()
						.username("TEST")
						.password("TEST")
						.pin("TEST")
						.build();
				portfolio.setCredentials(credentials);
				portfolio.setBroker(Broker.CONSOLE);
				portfolio.setMaxTradableAmount(1000000);
				portfolio.setName("TEST");
			}
		};
	}
}
