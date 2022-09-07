package com.whiteowl;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.whiteowl.core.user.User;
import com.whiteowl.core.user.UserService;

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
	
}
