package com.challenge.meli.dto;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Entity
@Table(name = "invocation")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "createdAt", "updatedAt" }, allowGetters = true)
public @Data class Invocation implements Serializable {

	private static final long serialVersionUID = -3095450263701634544L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	private String country;

	@Column
	private Double distance;

	@Column
	private Double numberRequests;

	public Invocation() {
		super();
	}

	public Invocation(@NotBlank String country, @NotBlank Double distance, @NotBlank Double numberRequests) {
		super();
		this.country = country;
		this.distance = distance;
		this.numberRequests = numberRequests;
	}

}
