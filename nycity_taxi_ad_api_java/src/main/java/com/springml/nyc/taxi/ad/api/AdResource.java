package com.springml.nyc.taxi.ad.api;

import com.springml.nyc.taxi.ad.api.model.Coupon;
import com.springml.nyc.taxi.ad.api.model.RideDetails;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

/**
 * Created by sam on 28/4/17.
 */
@RestController
@Api(value="Coupon Service", description="Fetches the coupon based on CloudML response")
public class AdResource {
    private static final Logger LOG = LoggerFactory.getLogger(AdResource.class);

    @Autowired
    private AdServer adServer;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ApiIgnore
    public String home() {
        return "Coupon Service is running!";
    }

    @RequestMapping(value = "/getCoupon", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<Coupon> getCoupon(@RequestBody RideDetails rideDetails) {
        LOG.info("getCoupon request: " + rideDetails);

        Coupon coupon = adServer.getCoupon(rideDetails);
        LOG.debug("getCoupon response " + coupon);

        return ResponseEntity.ok(coupon);
    }

    /**
     * <a href="https://cloud.google.com/appengine/docs/flexible/java/how-instances-are-managed#health_checking">
     * App Engine health checking</a> requires responding with 200 to {@code /_ah/health}.
     */
    @ApiIgnore
    @RequestMapping(value = "/_ah/health", method = RequestMethod.GET)
    public String healthy() {
        return "OK";
    }

}
