package com.challenge.meli.model;

import org.springframework.cache.annotation.Cacheable;

import lombok.Data;

@Cacheable("countries")
public @Data class Country {

	private String countryCode;
	private String countryCode3;
	private String countryName;
	private String countryEmoji;
}
