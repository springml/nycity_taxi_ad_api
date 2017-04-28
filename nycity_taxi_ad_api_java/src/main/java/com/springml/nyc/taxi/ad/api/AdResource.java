package com.springml.nyc.taxi.ad.api;

import com.springml.nyc.taxi.ad.api.model.RideDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return "Hello App Engine!";
    }

    @RequestMapping(value = "/getAd", method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<String> getAd(@RequestBody RideDetails rideDetails) {
        LOG.info("rideDetails : " + rideDetails);
        int adId = adServer.getAd(rideDetails);
        return ResponseEntity.ok("{\"adId\" : \"" + adId + "\"}");
    }

    /**
     * <a href="https://cloud.google.com/appengine/docs/flexible/java/how-instances-are-managed#health_checking">
     * App Engine health checking</a> requires responding with 200 to {@code /_ah/health}.
     */
    @RequestMapping("/_ah/health")
    public String healthy() {
        return "I am Healthy";
    }

}
