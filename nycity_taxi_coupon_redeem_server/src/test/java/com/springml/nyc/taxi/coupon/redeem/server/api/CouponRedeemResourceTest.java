package com.springml.nyc.taxi.coupon.redeem.server.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springml.nyc.taxi.ad.api.RedeemStatus;
import com.springml.nyc.taxi.ad.datastore.RedeemStoreManager;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponDetails;
import static org.hamcrest.Matchers.is;
import org.junit.Test;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import org.springframework.web.context.WebApplicationContext;
/**
 * Created by kaarthikraaj on 31/5/17.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = CouponRedeemApplication.class)
@WebAppConfiguration
public class CouponRedeemResourceTest {
    private static String ride_id = "ride-1001";
    private static String ad_id = "ad-1001";
    private static String successRedeemMessage = "SuccessFully Redeemed";
    private static String alreadyRedeemedMessage = "Failed -> AlreadyRedeemed";
    ;

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;
    @Autowired
    RedeemStoreManager redeemStoreManager;
    @Before
    public void setUp() {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
    }
    @Test
    public void testRedeemCoupon() throws Exception{

        redeemStoreManager.addCoupon(ride_id,ad_id);
        mockMvc.perform(post("/redeemCoupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getTestRedeemCouponDetails()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.ride_id", is(ride_id)))
                .andExpect(jsonPath("$.ad_id", is(ad_id)))
                .andExpect(jsonPath("$.redeemedStatus",is(successRedeemMessage)));
        mockMvc.perform(post("/redeemCoupon")
                .contentType(MediaType.APPLICATION_JSON)
                .content(getTestRedeemCouponDetails()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.ride_id", is(ride_id)))
                .andExpect(jsonPath("$.ad_id", is(ad_id)))
                .andExpect(jsonPath("$.redeemedStatus",is(alreadyRedeemedMessage)));
        redeemStoreManager.deleteCoupon(ride_id,ad_id);
    }

    private byte[] getTestRedeemCouponDetails() throws Exception {
        CouponDetails couponDetails = new CouponDetails();
        couponDetails.setAdId(ad_id);
        couponDetails.setRideId((ride_id));


        return new ObjectMapper().writeValueAsString(couponDetails).getBytes();
    }
}
