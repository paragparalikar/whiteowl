package com.whiteowl.core.derivative.option.chain;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.whiteowl.core.scrip.Index;
import com.whiteowl.core.scrip.Scrip;
import com.whiteowl.core.scrip.ScripService;

import lombok.RequiredArgsConstructor;

@Order
@Component
@RequiredArgsConstructor
public class OptionChainBuilderCommandLineRunner implements CommandLineRunner {

	private final ScripService scripService;
	private final OptionChainBuilder optionChainBuilder;
	
	@Override
	public void run(String... args) throws Exception {
		final LocalDate expiry = LocalDate.of(2023, 04, 6);
		final ZonedDateTime timestamp = ZonedDateTime.of(Year.now().getValue(), 
				Month.APRIL.getValue(), 3, 9, 18, 0, 0, ZoneId.systemDefault());
		final Scrip scrip = scripService.findByCode(Index.NIFTY50.getCode());
		final OptionChain optionChain = optionChainBuilder.build(scrip, expiry, timestamp);
		System.out.println(optionChain);
	}
	
}
