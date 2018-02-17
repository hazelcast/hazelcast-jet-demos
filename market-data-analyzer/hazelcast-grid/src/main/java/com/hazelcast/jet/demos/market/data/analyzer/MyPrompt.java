package com.hazelcast.jet.demos.market.data.analyzer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.shell.plugin.support.DefaultPromptProvider;
import org.springframework.stereotype.Component;

@Component
@Order(Integer.MIN_VALUE)
public class MyPrompt extends DefaultPromptProvider {

	@Value("${my.prompt}")
	private String prompt;
	
    @Override
    public final String getPrompt() {
    		return this.prompt + " cluster $ ";
    }
	
}
