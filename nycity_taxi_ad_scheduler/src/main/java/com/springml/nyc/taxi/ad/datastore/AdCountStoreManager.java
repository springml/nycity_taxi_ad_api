package com.springml.nyc.taxi.ad.datastore;


import autovalue.shaded.org.apache.commons.lang.StringUtils;
import com.google.cloud.datastore.*;

import java.util.Properties;
import java.util.logging.Logger;


/**
 * Manages the advertisement count store
 * All operations related to advenrtisement count store
 * are done through AdCountStoreManager
 * The list of operations supported are
 * a)Update entity that corresponds to current day's list of
 * display counts for each ad
 * b)Retreives the current day's display count for the advertisement id
 * passed
 */
public class AdCountStoreManager {
    //singleton instance of AdCountStoreManager
    private static AdCountStoreManager dataStoreManager;
    //Only datastore instance that AdcountManager access.
    private static final Datastore datastore;
    private Logger logger =
            Logger.getLogger(AdCountStoreManager.class.getName());
    //Entity's kind
    private final String kind = "AdvertisementDisplayCountHistory";

    static {
        datastore = DatastoreOptions.getDefaultInstance().getService();

    }

    /*
    Singleton instance returned
     */
    public static synchronized AdCountStoreManager getInstance() {

        if (dataStoreManager == null) {
            dataStoreManager = new AdCountStoreManager();
        }
        return dataStoreManager;
    }

    /*
    Updates entity that corresponds to current day's list of
    display counts for each ad
    @param  displayCountResponse This has the properties
                                 with advertisement id as keys and their count for
                                 the day as values
    The method is invoked by scheduler job after the count is retreived
    using big query
     */
    public void updateAdCountEntity(Properties displayCountResponse) {


        Key adCountEntityKey = getEntityKey();
        Entity.Builder entityBuilder = Entity.newBuilder(adCountEntityKey);

        for (Object propKeyObj : displayCountResponse.keySet()) {
            String propKey = (String) propKeyObj;
            entityBuilder.set(propKey, displayCountResponse.getProperty(propKey));
        }
        Entity entity = entityBuilder.build();
        datastore.put(entity);
        logger.info("AdCount is updated by scheduler successfully");
    }

    /*
    Gets the current day's display count for the advertisement requested
    @param adId The advertisement identifier for which the display count
                is requested for the current day which has the properties
    @param Th display count for the current day for the given adId
    This method is called during threshold limit check step
     */
    public int getAdUpdateCount(String adId) {
        int count = -1;
        Key adCountEntityKey = this.getEntityKey();
        Entity countStatsForToday = datastore.get(adCountEntityKey);
        try {
            String val = countStatsForToday.getString(adId);
            if (StringUtils.isNotEmpty(val) || StringUtils.isNotBlank(val)) {
                count = Integer.parseInt(val);
            }
        } catch (DatastoreException dataStoreException) {
            logger.warning("Exception while getting ad count entity" + dataStoreException.getMessage());
        }
        return count;
    }

    private Key getEntityKey() {
        Key entityKey;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String entityName = new StringBuilder(now.getYear()).append("-").append(now.getMonth()).append("-").append(now.getDayOfMonth()).toString();
        entityKey = datastore.newKeyFactory().setKind(kind).newKey(entityName);
        return entityKey;
    }
}
