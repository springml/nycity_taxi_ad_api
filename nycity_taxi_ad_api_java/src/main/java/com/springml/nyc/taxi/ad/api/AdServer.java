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

    public String getDiscount(int couponId) {
        String discount = discountProps.getProperty(Integer.toString(couponId));
        if (StringUtils.isBlank(discount)) {
            // Defaulting to 5%
            discount = "5%";
        }

        LOG.info("Discount " + discount);
        return discount;
    }

    private int getPredictedAd(RideDetails rideDetails) {
        try {
            GoogleCredential credential = GoogleCredential.getApplicationDefault()
                    .createScoped(Collections.singleton(CLOUDML_SCOPE));
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                    credential);
            GenericUrl url = new GenericUrl(predictRestUrl);

            JacksonFactory jacksonFactory = new JacksonFactory();
            JsonHttpContent jsonHttpContent = new JsonHttpContent(jacksonFactory, getPayLoad(rideDetails));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            jsonHttpContent.setWrapperKey("instances");
            jsonHttpContent.writeTo(baos);
            LOG.info("Executing request... " + baos.toString());
            HttpRequest request = requestFactory.buildPostRequest(url, jsonHttpContent);

            HttpResponse response = request.execute();
            String content = response.parseAsString();

            LOG.info("Got the following response from CloudML \n" + content);
            return getHighestProbabilityIndex(content);
        } catch (Exception e) {
            LOG.error("Error while executing CloudML", e);
        }

        return -1;
    }

    private List<Map<String, Object>> getPayLoad(RideDetails rideDetails) {
        List<Map<String, Object>> instances = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();

        map.put("passenger_count", rideDetails.getPassengerCount());
        map.put("tpep_pickup_datetime", rideDetails.getTpepPickupDatetime());
        map.put("pickup_latitude", rideDetails.getPickupLatitude());
        map.put("pickup_longitude", rideDetails.getPickupLongitude());
        map.put("dropoff_latitude", rideDetails.getDropoffLatitude());
        map.put("dropoff_longitude", rideDetails.getDropoffLongitude());

        instances.add(map);
        return instances;
    }

    private int getHighestProbabilityIndex(String content) {
        Gson gson = new Gson();
        Predictions predictions = gson.fromJson(content, Predictions.class);

        List<Prediction> predictionsList = predictions.getPredictions();
        List<Double> probabilities = predictionsList.get(0).getProbabilities();
        double max = Double.NEGATIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < probabilities.size(); i++) {
            Double prob = probabilities.get(i);
            if (prob > max) {
                max = prob;
                index = i;
            }
        }

        return index;
    }
}
