package com.springml.nyc.taxi.ad.api;

import com.springml.nyc.taxi.ad.api.model.Coupon;
import com.springml.nyc.taxi.ad.api.model.RideDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by sam on 28/4/17.
 */
@RestController
public class AdResource {
    private static final Logger LOG = LoggerFactory.getLogger(AdResource.class);

    @Autowired
    private AdServer adServer;
    @RequestMapping("/")
    public String home() {
        return "Coupon Service is running!";
    }

    @RequestMapping(value = "/getCoupon", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Coupon> getCoupon(@RequestBody RideDetails rideDetails) {
        LOG.info("getCoupon request: " + rideDetails);

        int couponId = adServer.getCoupon(rideDetails);
        int discount = adServer.getDiscount(couponId);

        Coupon response = new Coupon();
        response.setCouponId(couponId);
        response.setDiscountPercentage(discount);
        LOG.debug("getCoupon response " + response);

        return ResponseEntity.ok(response);
    }

    /**
     * <a href="https://cloud.google.com/appengine/docs/flexible/java/how-instances-are-managed#health_checking">
     * App Engine health checking</a> requires responding with 200 to {@code /_ah/health}.
     */
    @RequestMapping("/_ah/health")
    public String healthy() {
        return "OK";
    }

}
