package com.challenge.meli.controller;

import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.challenge.meli.dto.Invocation;
import com.challenge.meli.model.CoinDetail;
import com.challenge.meli.model.Country;
import com.challenge.meli.model.Currency;
import com.challenge.meli.model.InfoCountry;
import com.challenge.meli.model.IpAddress;
import com.challenge.meli.model.Language;

public abstract class ControllerUtilities extends MainController {

	/**
	 * Objeto para manejar los logs de la clase
	 */
	private static final Logger logger = LoggerFactory.getLogger(ControllerUtilities.class);

	private static ResponseEntity<Country> responseCountry;

	private static ResponseEntity<InfoCountry> responseInfoCountry;

	private static ResponseEntity<CoinDetail> responseCoinDetail;

	private static JSONObject jsonTrace;

	/**
	 * Method to consult the API api.ip2country.info, and obtain the basic
	 * information of the country of origin of the IP.
	 * 
	 * @param ip IP address of the country of origin.
	 * @return Country object with basic information about the country of origin.
	 */
	@Cacheable("countries")
	protected static Country getCountry(String ip) {

		if (responseCountry != null && jsonTrace != null) {
			if (!jsonTrace.get(ControllerConstants.IP).equals(ip)) {
				responseCountry = restTemplate().getForEntity(MainController.URL_API_COUNTRY_INFO_BY_IP_STATIC + ip,
						Country.class);
			}
		} else {
			responseCountry = restTemplate().getForEntity(MainController.URL_API_COUNTRY_INFO_BY_IP_STATIC + ip,
					Country.class);
		}

		return responseCountry.getBody();
	}

	/**
	 * Method to consult the restcountries.eu API, and obtain detailed information
	 * about the country of origin.
	 * 
	 * @param CountryIsoCode iso code of the country of origin.
	 * @return Detailed information on the country of origin.
	 */
	@Cacheable("infoCountry")
	protected static InfoCountry getInfoCountry(String countryIsoCode) {
		if (responseInfoCountry != null && responseInfoCountry.getBody() != null) {
			if (!responseInfoCountry.getBody().getAlpha3Code().equals(countryIsoCode)) {
				responseInfoCountry = restTemplate()
						.getForEntity(
								MainController.URL_API_DETAILED_COUNTRY_INFORMATION_STATIC + countryIsoCode
										+ MainController.DETAILED_COUNTRY_INFORMATION_PARAMETERS_STATIC,
								InfoCountry.class);
			}
		} else {
			responseInfoCountry = restTemplate().getForEntity(MainController.URL_API_DETAILED_COUNTRY_INFORMATION_STATIC
					+ countryIsoCode + MainController.DETAILED_COUNTRY_INFORMATION_PARAMETERS_STATIC,
					InfoCountry.class);
		}

		return responseInfoCountry.getBody();
	}

	/**
	 * Method to obtain the native language from the information of the country of
	 * origin.
	 * 
	 * @param infoCountry Detailed information of the country of origin.
	 * @return List of native languages of the country of origin.
	 */
	@Cacheable("nativeLanguages")
	protected static List<String> getNativeLanguages(InfoCountry infoCountry) {
		List<String> nativeLanguage = new ArrayList<>();
		for (Language language : infoCountry.getLanguages()) {
			logger.info(ControllerConstants.LANGUAGES + ": " + language.getNativeName());
			nativeLanguage.add(language.getNativeName());
		}
		return nativeLanguage;
	}

	/**
	 * Method to obtain the current hours from the information of the country of
	 * origin.
	 * 
	 * @param infoCountry Detailed information of the country of origin.
	 * @return List of current hours according to the time zones of the country of
	 *         origin.
	 */
	@Cacheable("currentHours")
	protected static List<String> getCurrentHours(InfoCountry infoCountry) {
		List<String> currentHours = new ArrayList<>();

		for (String timeZone : infoCountry.getTimezones()) {
			if (timeZone.equals(ControllerConstants.UTC)) {
				timeZone = ControllerConstants.UTC00;
			}
			ZoneOffset zoneOffSet = ZoneOffset.of(timeZone.replace(ControllerConstants.UTC, ControllerConstants.EMPTY));
			OffsetTime date = OffsetTime.now(zoneOffSet);
			DateTimeFormatter fmt = DateTimeFormatter
					.ofPattern(ControllerConstants.TIME_FORMAT + timeZone + ControllerConstants.SINGLE_QUOTE);

			currentHours.add(fmt.format(date));
		}

		return currentHours;
	}

	/**
	 * Method to calculate the distance between two coordinates of longitude and
	 * latitude.
	 * 
	 * @param latlng An array of type float that contains the longitude and latitude
	 *               coordinate of the country of origin.
	 * @return Distance value expressed in kilometers.
	 */
	@Cacheable("distanceBetweenCountries")
	protected static String getDistanceBetweenBsAsAndCountry(float[] latlng) {
		double countryLatitude = latlng[0];
		double countryLongitude = latlng[1];
		double bsAsLatitude = ControllerConstants.BSAS_LATITUDE;
		double bsAsLongitude = ControllerConstants.BSAS_LONGITUDE;
		double earthRadius = ControllerConstants.EARTH_RADIUS;
		double dLat = Math.toRadians(bsAsLatitude - countryLatitude);
		double dLng = Math.toRadians(bsAsLongitude - countryLongitude);
		double sindLat = Math.sin(dLat / 2);
		double sindLng = Math.sin(dLng / 2);
		double va1 = Math.pow(sindLat, 2) + Math.pow(sindLng, 2) * Math.cos(Math.toRadians(countryLatitude))
				* Math.cos(Math.toRadians(countryLongitude));
		double va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1));
		double distance = earthRadius * va2;
		return String.format(ControllerConstants.DECIMAL_FORMAT, distance);
	}

	/**
	 * Method to obtain the official currencies of the country of origin.
	 * 
	 * @param infoCountry Detailed information of the country of origin.
	 * @return Detail list of official currencies of the country of origin.
	 */
	@Cacheable("coinDetail")
	protected static List<CoinDetail> getCoinValue(InfoCountry infoCountry) {

		List<CoinDetail> coinValueList = new ArrayList<>();

		if (responseCoinDetail != null && responseCoinDetail.getBody() != null
				&& responseCoinDetail.getBody().getRates() != null) {
			if (!responseCoinDetail.getBody().getRates().keySet().toString()
					.replace(ControllerConstants.OPEN_BRACKET, ControllerConstants.EMPTY)
					.replace(ControllerConstants.CLOSED_BRACKET, ControllerConstants.EMPTY)
					.equals(infoCountry.getAlpha3Code())) {

				for (Currency currency : infoCountry.getCurrencies()) {
					responseCoinDetail = restTemplate().getForEntity(MainController.API_URL_COIN_DETAIL_STATIC
							+ currency.getCode() + MainController.API_URL_COIN_DETAIL_COMPLEMENT_STATIC,
							CoinDetail.class);
					coinValueList.add(responseCoinDetail.getBody());
				}

			}
		} else {
			for (Currency currency : infoCountry.getCurrencies()) {
				responseCoinDetail = restTemplate().getForEntity(MainController.API_URL_COIN_DETAIL_STATIC
						+ currency.getCode() + MainController.API_URL_COIN_DETAIL_COMPLEMENT_STATIC, CoinDetail.class);
				coinValueList.add(responseCoinDetail.getBody());
			}
		}

		return coinValueList;
	}

	/**
	 * Method to populate the json object that the endpoint trace will respond to.
	 * 
	 * @param netAddress                    Source ip address.
	 * @param country                       Country of origin.
	 * @param infoCountry                   Information of the country of origin.
	 * @param nativeLanguage                Native languages of the country of
	 *                                      origin.
	 * @param currentHours                  Current hours of the country of origin.
	 * @param distanceBetweenBsAsAndCountry Linear distance between BsAs and the
	 *                                      country of origin.
	 * @param coinValueList                 List of official currencies of the
	 *                                      country of origin.
	 * @return Json object populated with the data from the country of origin.
	 */
	@Cacheable("jsonTrace")
	protected static JSONObject populateJsonObjectToGetTrace(IpAddress netAddress, Country country,
			InfoCountry infoCountry, List<String> nativeLanguage, List<String> currentHours,
			Double distanceBetweenBsAsAndCountry, List<CoinDetail> coinValueList) {
		jsonTrace = new JSONObject();
		jsonTrace.put(ControllerConstants.IP, netAddress.getIp());
		jsonTrace.put(ControllerConstants.COUNTRY, country.getCountryName());
		jsonTrace.put(ControllerConstants.COUNTRY_ISO_CODE, country.getCountryCode());
		jsonTrace.put(ControllerConstants.LANGUAGES, nativeLanguage);
		jsonTrace.put(ControllerConstants.TIMES, currentHours);
		jsonTrace.put(ControllerConstants.ESTIMATED_DISTANCE,
				distanceBetweenBsAsAndCountry + ControllerConstants.KM_UNIT);

		for (CoinDetail coinDetail : coinValueList) {
			String isoCodeCurrency = coinDetail.getRates().keySet().toString()
					.replace(ControllerConstants.OPEN_BRACKET, ControllerConstants.EMPTY)
					.replace(ControllerConstants.CLOSED_BRACKET, ControllerConstants.EMPTY);

			jsonTrace.put(ControllerConstants.CURRENCY,
					isoCodeCurrency + ControllerConstants.A_ONE_EURO_LABEL + coinDetail.getRates().get(isoCodeCurrency)
							+ ControllerConstants.SPACE_SEPARATOR
							+ infoCountry.getCurrencies().get(ControllerConstants.NUMBER_ZERO).getSymbol()
							+ ControllerConstants.CLOSING_PARENTHESIS_SEPARATOR);
		}
		return jsonTrace;
	}

	/**
	 * Method that obtains the average distance from where the requests have been
	 * made.
	 * 
	 * @param invocations List of requests executed.
	 * @return Average value of the distance from where the requests have been made.
	 */
	@Cacheable("averageDistance")
	protected static String getAverageDistanceByInvocations(List<Invocation> invocations) {
		Double averageDistance = 0.0;
		Double divider = 0.0;
		for (Invocation invocation : invocations) {
			divider += invocation.getNumberRequests();
			logger.info("id: " + invocation.getId() + " ; divider after: " + divider);
			averageDistance += Double.valueOf(invocation.getDistance()) * invocation.getNumberRequests();
			logger.info("id: " + invocation.getId() + " ; averageDistance after:" + averageDistance);
		}

		logger.info("averageDistance total: " + averageDistance);
		logger.info("divider total: " + divider);

		return String.format(ControllerConstants.DECIMAL_FORMAT, averageDistance / divider);
	}

	/**
	 * Method to populate the json object that the stats endpoint will respond to.
	 * 
	 * @param invocations     Invocations List of requests executed.
	 * @param averageDistance Average value of the distance from where the requests
	 *                        have been made.
	 * @return Json object populated with the request statistics data
	 */
	@Cacheable("jsonStats")
	protected static JSONObject populateJsonObjectToGetStatistics(List<Invocation> invocations,
			String averageDistance) {
		JSONObject jsonStats = new JSONObject();
		jsonStats.put(ControllerConstants.FARTHEST_COUNTRY, invocations.get(ControllerConstants.NUMBER_ZERO));
		jsonStats.put(ControllerConstants.NEARBY_COUNTRY,
				invocations.get(invocations.size() - ControllerConstants.NUMBER_ONE));
		jsonStats.put(ControllerConstants.AVERAGE_DISTANCE, averageDistance);
		return jsonStats;
	}

	@Bean
	protected static RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
