package com.challenge.meli.model;

import java.util.HashMap;

import org.springframework.cache.annotation.Cacheable;

import lombok.Data;

@Cacheable("coinDetail")
public @Data class CoinDetail {
	
	private boolean success;
	private Double timestamp;
	private String base;
	private String date;
	private HashMap<String, Object> rates;

}
