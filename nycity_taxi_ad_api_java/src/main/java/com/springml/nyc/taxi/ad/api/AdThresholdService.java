package com.springml.nyc.taxi.ad.api;

import com.springml.nyc.taxi.ad.datastore.AdCountStoreManager;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;


/**
 * This class exposes APIs to get all operations
 * related to threshold for every ad
 * As of now the following operation(s) supported is
 * a)Check if the limit for advertisement display count is reached
 */
public class AdThresholdService {
    @Autowired
    private static AdThresholdService adThresholdService;

    private String adThresholdFilename;
    private static Properties adCountThresholdLimitRegistry = new Properties();
    private static Logger logger =
            Logger.getLogger(AdThresholdService.class.getName());
    //limit current set same for all ads.
    //this will be replaced by property file

    @Value("${coupons.threshold.limit.file}")
    private String adThresholdFileName;


    /*
    Private constructor to implement singleton
     */
    private AdThresholdService() {

    }

    /*Singleton implementation as its good enough to have single instance
    for the scenario
    */
    public static synchronized AdThresholdService getInstance() {

        if (adThresholdService == null) {
            adThresholdService = new AdThresholdService();
        }
        return adThresholdService;
    }

    /*Checks if the limit for the ad display count is reached
    */
    public boolean isAdThresholdExceeded(int advId) {
        boolean isThresholdExceeded = false;
        int count = AdCountStoreManager.getInstance().getAdUpdateCount(Integer.toString(advId));
        long limit = 0;
        Object advIdThreholdProp = adCountThresholdLimitRegistry.getProperty(Integer.toString(advId));
        if (advIdThreholdProp != null) {
            limit = Long.parseLong(advIdThreholdProp.toString());
            logger.info("the current limit set for the ad Id" + advId + "is" + limit);
            if (count >= limit)
                isThresholdExceeded = true;
        }
        return isThresholdExceeded;
    }


    public static void main(String[] args) {
        AdThresholdService adThresholdService = AdThresholdService.getInstance();
        System.out.println("is Exceeded" + adThresholdService.isAdThresholdExceeded(6));
    }

    /*Ad Threshold limit is read from properties file and loaded in this instance.
    */
    @PostConstruct
    public void loadAdCountThresholdLimit() {
        java.io.InputStream adThresholdLimitInputSt = AdThresholdService.class.getClassLoader().getResourceAsStream(adThresholdFileName);
        if (adThresholdLimitInputSt != null) {
            try {
                adCountThresholdLimitRegistry.load(adThresholdLimitInputSt);
            } catch (IOException ioException) {
                logger.warning("AdCountThreshold limit file not found");
            }
        }

    }

}
