package com.springml.nyc.taxi.ad.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.springml.nyc.taxi.ad.api.model.Coupon;
import com.springml.nyc.taxi.ad.api.model.Prediction;
import com.springml.nyc.taxi.ad.api.model.Predictions;
import com.springml.nyc.taxi.ad.api.model.RideDetails;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int DEFAULT_COUPON_ID = 2;

    @Value("${cloudML.predict.rest.url}")
    private String predictRestUrl;

    @Value("${coupons.json.file}")
    private String couponsJsonFile;

    private List<Coupon> coupons;

    @PostConstruct
    private void initDiscountProperties() throws IOException {
        InputStream jsonIs = this.getClass().getClassLoader().getResourceAsStream(couponsJsonFile);
        String json = IOUtils.toString(jsonIs, Charset.defaultCharset());

        Type listType = new TypeToken<List<Coupon>>() {}.getType();
        coupons = new ObjectMapper().readValue(json.getBytes(),
                new TypeReference<List<Coupon>>() {
        });

        LOG.info("Loaded coupons from " + couponsJsonFile);
        LOG.info(coupons.toString());
    }

    public Coupon getCoupon(RideDetails rideDetails) {
        int couponId = getCouponId(rideDetails);
        return getCoupon(couponId);
    }


    public void setPredictRestUrl(String predictRestUrl) {
        this.predictRestUrl = predictRestUrl;
    }

    private Coupon getCoupon(int couponId) {
        for (Coupon coupon : coupons) {
            if (coupon.getCouponId() == couponId) {
                return coupon;
            }
        }

        // No coupon matches
        LOG.info("No coupon matches couponId " + couponId);
        return getDefaultCoupon();
    }

    private Coupon getDefaultCoupon() {
        // Default Coupon
        /**
         {
             "coupon_id": 2,
             "discount_percentage": 10,
             "business_type": "Oil Change - Auto Maintanance",
             "business_name": "AutoShop SR",
             "coupon_image": "http://coupon-service-taxi.image.com/Autoshop_ad.jpg"
         }
         */
        return getCoupon(DEFAULT_COUPON_ID);
    }

    private int getCouponId(RideDetails rideDetails) {

        try {
            GoogleCredential credential = GoogleCredential.getApplicationDefault()
                    .createScoped(Collections.singleton(CLOUDML_SCOPE));
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                    credential);
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

        // Return default coupon in case of error
        return DEFAULT_COUPON_ID;
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
