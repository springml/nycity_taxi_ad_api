package com.springml.nyc.taxi.ad.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springml.nyc.taxi.ad.api.model.RideDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
/**
 * Created by sam on 2/5/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AdApiApplication.class)
@WebAppConfiguration
public class AdResourceTest {
    private static final String INVALID_PREDICT_SERVICE_URL =
            "https://ml.googleapis.com/v1beta1/projects/billion-taxi-rides/models/invalid/versions/v4:predict";
    private static final String VALID_PREDICT_SERVICE_URL =
            "https://ml.googleapis.com/v1beta1/projects/billion-taxi-rides/models/taxiadz/versions/v4:predict";

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AdServer adServer;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("Coupon Service is running!"));
    }

    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/_ah/health"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void testGetCoupon() throws Exception {
        adServer.setPredictRestUrl(VALID_PREDICT_SERVICE_URL);
        mockMvc.perform(post("/getCoupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getTestRideDetails()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.coupon_id", is(3)))
                .andExpect(jsonPath("$.discount_percentage", is(4)))
                .andExpect(jsonPath("$.business_type", is("Retail")))
                .andExpect(jsonPath("$.business_name", is("Retail For You")))
                .andExpect(jsonPath("$.coupon_image", is("http://coupon-service-taxi.image.com/Retail_ad.jpg")));
    }

    @Test
    public void testDefaultCouponOnError() throws Exception {
        adServer.setPredictRestUrl(INVALID_PREDICT_SERVICE_URL);
        mockMvc.perform(post("/getCoupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getTestRideDetails()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.coupon_id", is(2)))
                .andExpect(jsonPath("$.discount_percentage", is(10)))
                .andExpect(jsonPath("$.business_type", is("Oil Change - Auto Maintanance")))
                .andExpect(jsonPath("$.business_name", is("AutoShop SR")))
                .andExpect(jsonPath("$.coupon_image", is("http://coupon-service-taxi.image.com/Autoshop_ad.jpg")));
    }

    private byte[] getTestRideDetails() throws JsonProcessingException {
        RideDetails rideDetails = new RideDetails();
        rideDetails.setPassengerCount(4);
        rideDetails.setTpepPickupDatetime("2015-01-05 02:06:55");
        rideDetails.setPickupLatitude(41.851056671142578f);
        rideDetails.setPickupLongitude(-74.994209289550781f);
        rideDetails.setDropoffLatitude(42.749996185302734f);
        rideDetails.setDropoffLongitude(-73.97900390625f);

        return new ObjectMapper().writeValueAsString(rideDetails).getBytes();
    }

}
