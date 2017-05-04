package com.springml.nyc.taxi.ad.api;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.Gson;
import com.springml.nyc.taxi.ad.api.model.Prediction;
import com.springml.nyc.taxi.ad.api.model.Predictions;
import com.springml.nyc.taxi.ad.api.model.RideDetails;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by sam on 28/4/17.
 */
public class AdServer {
    private static final Logger LOG = LoggerFactory.getLogger(AdResource.class);

    private static final String CLOUDML_SCOPE =
            "https://www.googleapis.com/auth/cloud-platform";
    private static final String PASSENGER_COUNT = "passenger_count";
    private static final String TPEP_PICKUP_DATETIME = "tpep_pickup_datetime";
    private static final String PICKUP_LATITUDE = "pickup_latitude";
    private static final String PICKUP_LONGITUDE = "pickup_longitude";
    private static final String DROPOFF_LATITUDE = "dropoff_latitude";
    private static final String DROPOFF_LONGITUDE = "dropoff_longitude";

    @Value("${cloudML.predict.rest.url}")
    private String predictRestUrl;

    @Value("${coupon.discount.map.file}")
    private String discountMapFile;

    private Properties discountProps;

    @PostConstruct
    private void initDiscountProperties() throws IOException {
        discountProps = new Properties();
        discountProps.load(this.getClass().getClassLoader().getResourceAsStream(discountMapFile));

        LOG.info("Loaded coupon discount mapping");
        LOG.info(discountProps.toString());
    }

    public int getCoupon(RideDetails rideDetails) {
        return getPredictedAd(rideDetails);
    }

    public int getDiscount(int couponId) {
        String discountStr = discountProps.getProperty(Integer.toString(couponId));
        if (StringUtils.isBlank(discountStr)) {
            // Defaulting to 5%
            discountStr = "5";
        }

        LOG.info("Discount " + discountStr);
        return Integer.parseInt(discountStr);
    }

    private int getPredictedAd(RideDetails rideDetails) {
        try {
            GoogleCredential credential = GoogleCredential.getApplicationDefault()
                    .createScoped(Collections.singleton(CLOUDML_SCOPE));
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestInitializer requestInitializer = request -> {
                credential.initialize(request);
                //TODO : Use exponential backup
                request.setReadTimeout(0);
            };

            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                    requestInitializer);

            GenericUrl url = new GenericUrl(predictRestUrl);

            JacksonFactory jacksonFactory = new JacksonFactory();
            JsonHttpContent jsonHttpContent = new JsonHttpContent(jacksonFactory, getRideDetailsAsCloudMLRequest(rideDetails));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            jsonHttpContent.setWrapperKey("instances");
            jsonHttpContent.writeTo(baos);
            LOG.info("Executing CloudML predictions with payload : " + baos.toString());
            HttpRequest request = requestFactory.buildPostRequest(url, jsonHttpContent);

            HttpResponse response = request.execute();
            String predictionResponse = response.parseAsString();

            LOG.info("CloudML prediction response \n" + predictionResponse);
            return getCouponWithHighProbability(predictionResponse);
        } catch (Exception e) {
            LOG.error("Error while getting predictions using CloudML", e);
        }

        return -1;
    }

    private List<Map<String, Object>> getRideDetailsAsCloudMLRequest(RideDetails rideDetails) {
        List<Map<String, Object>> instances = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();

        map.put(PASSENGER_COUNT, rideDetails.getPassengerCount());
        map.put(TPEP_PICKUP_DATETIME, rideDetails.getTpepPickupDatetime());
        map.put(PICKUP_LATITUDE, rideDetails.getPickupLatitude());
        map.put(PICKUP_LONGITUDE, rideDetails.getPickupLongitude());
        map.put(DROPOFF_LATITUDE, rideDetails.getDropoffLatitude());
        map.put(DROPOFF_LONGITUDE, rideDetails.getDropoffLongitude());

        instances.add(map);
        return instances;
    }

    private int getCouponWithHighProbability(String content) {
        Gson gson = new Gson();
        Predictions predictions = gson.fromJson(content, Predictions.class);

        List<Prediction> predictionsList = predictions.getPredictions();
        List<Double> probabilities = predictionsList.get(0).getProbabilities();
        double maxProbability = Double.NEGATIVE_INFINITY;
        int couponId = -1;
        for (int i = 0, size = probabilities.size(); i < size; i++) {
            Double prob = probabilities.get(i);
            if (prob > maxProbability) {
                maxProbability = prob;
                couponId = i;
            }
        }

        return couponId;
    }
}
