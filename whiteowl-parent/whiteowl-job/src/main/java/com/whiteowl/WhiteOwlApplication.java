package com.whiteowl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@EnableRetry
@EnableCaching
@EnableScheduling
@EnableJpaAuditing
@EnableTransactionManagement
@EnableConfigurationProperties
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@SpringBootApplication(exclude = ErrorMvcAutoConfiguration.class)
public class WhiteOwlApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(WhiteOwlApplication.class, args);
	}
	
	@Bean
	public ObjectMapper objectMapper() {
		return JsonMapper.builder()
			    .addModule(new JavaTimeModule())
			    .build();
	}
}
