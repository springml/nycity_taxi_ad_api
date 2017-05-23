package com.springml.nyc.taxi.ad.datastore;

import com.google.cloud.spanner.*;
import com.springml.nyc.taxi.ad.api.RedeemStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Manages the coupon redeem  store
 * All operations related to coupon redeem  store
 * are done through RedeemStoreManager
 * The list of operations supported are
 * a)Add coupon entry to redeem store
 * b)get current coupon redeem status
 * c)redeem coupon
 */
public class RedeemStoreManager {
    private static final Logger LOG = LoggerFactory.getLogger(RedeemStoreManager.class);
    //singleton instance of RedeemStoreManager
    private static RedeemStoreManager redeemStoreMgr;
    //Spanner instance id for our project
    private final String spannerInstanceId = "sml-coupon-redeem-store";
    //database that represents redeem store
    private final String databaseName = "sml_coupon_redeem_store";
    private DatabaseClient client;
    //Table in the database which has coupon redeem info
    private final String tableName = "coupon_ride_ad";
    private static Spanner spanner;

    static {
        try {

            spanner = SpannerOptions.getDefaultInstance().getService();
        } catch (Exception e) {
            LOG.error("Error while initialising RedeemStoreManager");
        }
    }


    private RedeemStoreManager() {

    }

    /*
    Singleton instance returned
     */
    public synchronized static RedeemStoreManager getInstance() {
        if (redeemStoreMgr == null) {
            redeemStoreMgr = new RedeemStoreManager();
        }
        return redeemStoreMgr;
    }

    private DatabaseClient getClient() {
        if (client == null) {
            DatabaseId id = DatabaseId.of(spanner.getOptions().getProjectId(), spannerInstanceId, databaseName);
            client = spanner.getDatabaseClient(id);
        }
        return client;
    }

    /*
    Adds coupon information along with its redeem status
    @param  rideId Ride id that contains details like pickup,drop location,no of passengers etc
    @param adId Advertisement ID which is to be displayed in that ride
    The method is invoked while executing getCoupon service job after the count is retreived
    using big query
     */
    public boolean addCoupon(String rideId, String adId) {
        boolean response = false;
        try {
            ArrayList<Mutation> mutations = new ArrayList<Mutation>();
            Mutation mutuation = Mutation.newInsertBuilder(tableName)
                    .set("ride_Id")
                    .to(rideId)
                    .set("ad_Id")
                    .to(adId)
                    .set("availed")
                    .to(false).build();
            mutations.add(mutuation);
            getClient().write(mutations);
            response = true;
        } catch (SpannerException exc) {
            LOG.error("Exception while adding coupon entry to redeem store" + exc.getMessage());
            response = false;
        }
        return response;
    }

    /*
    Gets coupon's redeem status
    @param  rideId Ride id that contains details like pickup,drop location,no of passengers etc
    @param adId Advertisement ID which is to be displayed in that ride
    The method is invoked while executing getCoupon service job after the count is retreived
    using big query
     */
    public RedeemStatus getRedeemStatus(String rideId, String adId) {
        try (ReadOnlyTransaction readOnly = getClient().readOnlyTransaction()) { //readWriteTransaction()) {

            Struct couponredeemedResult = readOnly.readRow("coupon_ride_ad", Key.of(rideId, adId), Arrays.asList("availed"));

            if (couponredeemedResult == null) {
                return RedeemStatus.NONEXIST;
            } else {
                boolean redeemed = couponredeemedResult.getBoolean("availed");
                if (redeemed) {
                    return RedeemStatus.REDEEMED;
                } else {
                    return RedeemStatus.NOTREDEEMED;
                }
            }
        } catch (SpannerException exc) {
            LOG.error("Exception while getting redeem status so returning NOT REDEEMED" + exc.getMessage());
            return RedeemStatus.NOTREDEEMED;
        }
    }

    /*
    Redeems the coupon
    @param  rideId Ride id that contains details like pickup,drop location,no of passengers etc
    @param adId Advertisement ID which is to be displayed in that ride
    The method is supposed to be invoked by redeemCoupon service
    Currenty since the readWrite transaction does not give a handler to check the status of transaction
    this method is not used as of now
     */
    public boolean redeemCoupon(String rideId, String adId) {
        boolean response = false;

        TransactionRunner runner = getClient().readWriteTransaction();
        // TransactionOptions.ReadWrite readWriteOption = TransactionOptions.getDefaultInstance().getReadWrite();
        //readWriteOption.toBuilder().
        runner.run(new TransactionRunner.TransactionCallable<Void>() {
            @Override
            public Void run(TransactionContext transaction) throws Exception {
                // Transfer marketing budget from one album to another. We do it in a transaction to
                // ensure that the transfer is atomic
                //
                // transaction.
                boolean isCouponredeemed = transaction.readRow("coupon_ride_ad", Key.of(rideId, adId), Arrays.asList("availed")).getBoolean("availed");
                if (!isCouponredeemed) {
                    transaction.buffer(Mutation.newUpdateBuilder(tableName)
                            .set("ride_Id").to(rideId)
                            .set("ad_Id").to(adId)
                            .set("availed").to(true).build());
                }


                return null;
            }

        });
        if (runner.getCommitTimestamp() != null)
            response = true;
        else {
            response = false;
        }
        return response;
    }

    /*
    Redeems the coupon
    @param  rideId Ride id that contains details like pickup,drop location,no of passengers etc
    @param adId Advertisement ID which is to be displayed in that ride
    The method is invoked by redeemCoupon service
     */
    public boolean redeemCouponNonAtomic(String rideId, String adId) {
        boolean success = false;

        ReadOnlyTransaction readOnly = getClient().readOnlyTransaction();//readWriteTransaction();
        boolean isCouponredeemed = readOnly.readRow("coupon_ride_ad", Key.of(rideId, adId), Arrays.asList("availed")).getBoolean("availed");
        if (!isCouponredeemed) {

            ArrayList<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(Mutation.newUpdateBuilder(tableName)
                    .set("ride_Id").to(rideId)
                    .set("ad_Id").to(adId)
                    .set("availed").to(true).build());
            getClient().write(mutations);
            success = true;
        }
        readOnly.close();
        LOG.info("coupon redemption succeed? " + success);
        return success;
    }

    public static void main(String s[]) {
        RedeemStoreManager redeemStoreManager = RedeemStoreManager.getInstance();
        redeemStoreManager.addCoupon("ride-1001", "ad-101");
        boolean redeemed = redeemStoreManager.redeemCouponNonAtomic("ride-1001", "ad-101");
        System.out.println("is coupon redeemed first call" + redeemed);
        redeemed = redeemStoreManager.redeemCouponNonAtomic("ride-1001", "ad-101");
        System.out.println("is coupon redeemed second call" + redeemed);
        System.out.println(redeemStoreManager.getRedeemStatus("ride-1002", "ad-101"));


        // System.exit(0);
    }
}
