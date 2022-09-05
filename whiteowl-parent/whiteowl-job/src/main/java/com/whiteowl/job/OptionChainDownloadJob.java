package com.whiteowl.job;

import org.springframework.stereotype.Component;

import com.whiteowl.core.derivative.option.OptionChainProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OptionChainDownloadJob {

	private final OptionChainProvider optionChainProvider;

}
