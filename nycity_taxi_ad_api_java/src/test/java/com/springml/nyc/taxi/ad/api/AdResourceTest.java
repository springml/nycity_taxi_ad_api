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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
/**
 * Created by sam on 2/5/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = AdApiApplication.class)
@WebAppConfiguration
public class AdResourceTest {
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("Hello App Engine!"));
    }

    @Test
    public void testHealthCheck() throws Exception {
        mockMvc.perform(get("/_ah/health"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().string("I am Healthy"));
    }

    @Test
    public void testGetCoupon() throws Exception {
        mockMvc.perform(post("/getCoupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getTestRideDetails()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.couponId", is(2)))
                .andExpect(jsonPath("$.discount", is("10%")));
    }

    private byte[] getTestRideDetails() throws JsonProcessingException {
        RideDetails rideDetails = new RideDetails();
        rideDetails.setPassengerCount(2);
        rideDetails.setTpepPickupDatetime("2015-01-04 20:06:55");
        rideDetails.setPickupLatitude(40.751056671142578f);
        rideDetails.setPickupLongitude(-73.994209289550781f);
        rideDetails.setDropoffLatitude(40.749996185302734f);
        rideDetails.setDropoffLongitude(-73.97900390625f);

        return new ObjectMapper().writeValueAsString(rideDetails).getBytes();
    }
}
