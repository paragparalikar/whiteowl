package com.whiteowl.core.quote;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.experimental.Delegate;

@Service
public class DefaultQuoteService implements QuoteService {
	
	@Autowired @Delegate private QuoteDataProvider quoteDataProvider;
	
}
