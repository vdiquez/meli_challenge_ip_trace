package com.challenge.meli.controller;

import java.util.List;

import javax.persistence.NoResultException;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.challenge.meli.dto.Invocation;
import com.challenge.meli.model.CoinDetail;
import com.challenge.meli.model.Country;
import com.challenge.meli.model.InfoCountry;
import com.challenge.meli.model.IpAddress;
import com.challenge.meli.repository.InvocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class MainController {

	/**
	 * Objeto para manejar los logs de la clase
	 */
	private static final Logger logger = LoggerFactory.getLogger(MainController.class);

	/**
	 * URL cargada desde el archivo de propiedades
	 */
	protected static String URL_API_COUNTRY_INFO_BY_IP_STATIC;

	/**
	 * URL cargada desde el archivo de propiedades
	 */
	protected static String URL_API_DETAILED_COUNTRY_INFORMATION_STATIC;

	/**
	 * Parametros cargados desde el archivo de propiedades
	 */
	protected static String DETAILED_COUNTRY_INFORMATION_PARAMETERS_STATIC;

	/**
	 * URL cargada desde el archivo de propiedades
	 */
	protected static String API_URL_COIN_DETAIL_STATIC;

	/**
	 * Complemento cargado desde el archivo de propiedades
	 */
	protected static String API_URL_COIN_DETAIL_COMPLEMENT_STATIC;

	/**
	 * Object used to save information about requests made by users.
	 */
	private static Invocation invocation;

	/**
	 * Interface used to perform actions on the database.
	 */
	@Autowired
	private InvocationRepository invocationRepository;

	/**
	 * Endpoint that obtains information about the country of origin, its language,
	 * currency, current time from the client's IP address.
	 * 
	 * @param netAddress object containing the source ip.
	 * @return json object with detailed information obtained from the client's ip.
	 */
	@ResponseBody
	@PostMapping(path = "/trace", consumes = "application/json", produces = "application/json")
	public String trace(@RequestBody IpAddress netAddress, BindingResult brs) {
		logger.info("ip address: " + netAddress.getIp());
		
		JSONObject json;

		final InetAddressValidator validator = InetAddressValidator.getInstance();
		if (validator.isValidInet4Address(netAddress.getIp())) {
			logger.info("ip valida");

			Country country = ControllerUtilities.getCountry(netAddress.getIp());

			logger.info(ControllerConstants.COUNTRY + ": " + country.getCountryName());
			logger.info(ControllerConstants.COUNTRY_ISO_CODE + ": " + country.getCountryCode());

			InfoCountry infoCountry = ControllerUtilities.getInfoCountry(country.getCountryCode3());

			List<String> nativeLanguage = ControllerUtilities.getNativeLanguages(infoCountry);

			List<String> currentHours = ControllerUtilities.getCurrentHours(infoCountry);

			Double distanceBetweenBsAsAndCountry = Double
					.valueOf(ControllerUtilities.getDistanceBetweenBsAsAndCountry(infoCountry.getLatlng()));

			logger.info(ControllerConstants.ESTIMATED_DISTANCE + ": " + distanceBetweenBsAsAndCountry
					+ ControllerConstants.KM_UNIT);

			List<CoinDetail> coinValueList = ControllerUtilities.getCoinValue(infoCountry);

			boolean persistenceResult = persistRequestInformation(country.getCountryName(),
					distanceBetweenBsAsAndCountry);

			json = ControllerUtilities.populateJsonObjectToGetTrace(netAddress, country, infoCountry,
					nativeLanguage, currentHours, distanceBetweenBsAsAndCountry, coinValueList);

			logger.info("persistenceResult: " + persistenceResult);

		} else {
			logger.info("ip NO valida - bad request");
			json = new JSONObject();
			json.put("message", "bad request");
		}

		return json.toString();
	}

	/**
	 * Endpoint that supplies the statistics about the requests.
	 * 
	 * @return Json object with information about the distances from where the
	 *         queries are made and the average of the same.
	 */
	@ResponseBody
	@GetMapping(path = "/stats", produces = "application/json")
	public String stats() {
		logger.info("stats START");

		List<Invocation> invocations = getInvocations();

		String averageDistance = ControllerUtilities.getAverageDistanceByInvocations(invocations);

		JSONObject json = ControllerUtilities.populateJsonObjectToGetStatistics(invocations, averageDistance);

		return json.toString();
	}

	/**
	 * Method used to save or update the information of a request.
	 * 
	 * @param countryName                   name of the country of origin.
	 * @param distanceBetweenBsAsAndCountry linear distance between country of
	 *                                      origin and BsAs.
	 * @return Value true if the process is successful and false if it is not.
	 */
	private boolean persistRequestInformation(String countryName, Double distanceBetweenBsAsAndCountry) {
		logger.info("persistRequestInformation START - countryName: " + countryName);
		boolean result = false;
		try {
			invocation = invocationRepository.findByCountry(countryName);
		} catch (NoResultException nRE) {
			invocation = null;
			System.err.println(
					"ERROR findCountryByName - countryName: " + countryName + " ; message: " + nRE.getMessage());
		}

		if (invocation != null) {
			invocation.setNumberRequests(invocation.getNumberRequests() + 1);
		} else {
			invocation = new Invocation(countryName, distanceBetweenBsAsAndCountry, 1.0);

		}

		invocationRepository.save(invocation);
		result = true;
		return result;
	}

	/**
	 * Method that gets the invocations ordered in descending order based on
	 * distance.
	 * 
	 * @return Invocations list.
	 */
	@Cacheable("invocations")
	protected List<Invocation> getInvocations() {
		List<Invocation> invocations = invocationRepository.findAllByOrderByDistanceDesc();
		return invocations;
	}

	@Value("${url.api.country.info.by.ip}")
	public void setUrlApiCountryInfoByIpStatic(String urlApiCountryInfoByIP) {
		MainController.URL_API_COUNTRY_INFO_BY_IP_STATIC = urlApiCountryInfoByIP;
	}

	@Value("${url.api.detailed.country.information}")
	public void setUrlApiDetailedCountryInformationStatic(String urlApiDetailedCountryInformation) {
		MainController.URL_API_DETAILED_COUNTRY_INFORMATION_STATIC = urlApiDetailedCountryInformation;
	}

	@Value("${detailed.country.information.parameters}")
	public void setDetailedCountryInformationParametersStatic(String detailedCountryInformationParameters) {
		MainController.DETAILED_COUNTRY_INFORMATION_PARAMETERS_STATIC = detailedCountryInformationParameters;
	}

	@Value("${api.url.coin.detail}")
	public void setApiUrlCoinDetailStatic(String apiUrlCoinDetail) {
		MainController.API_URL_COIN_DETAIL_STATIC = apiUrlCoinDetail;
	}

	@Value("${api.url.coin.detail.complement}")
	public void setapiUrlCoinDetailComplementStatic(String apiUrlCoinDetailComplement) {
		MainController.API_URL_COIN_DETAIL_COMPLEMENT_STATIC = apiUrlCoinDetailComplement;
	}

}
