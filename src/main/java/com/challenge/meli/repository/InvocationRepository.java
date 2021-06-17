package com.challenge.meli.repository;

import java.util.List;

import javax.persistence.NoResultException;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.challenge.meli.dto.Invocation;

@Repository
public interface InvocationRepository extends JpaRepository<Invocation, Long> {
	
	public List<Invocation> findAllByOrderByDistanceDesc();
	
	public Invocation findByCountry(String countryName) throws NoResultException;

}
