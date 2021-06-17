package com.challenge.meli.model;

import java.util.List;

import org.springframework.cache.annotation.Cacheable;

import lombok.Data;

@Cacheable("infoCountry")
public @Data class InfoCountry {
	
	private List<Currency> currencies;
	private List<Language> languages;
	private String name;
	private float[] latlng;
	private List<String> timezones;
	private String alpha3Code;

}
