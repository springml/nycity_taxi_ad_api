package com.springml.nyc.taxi.ad.datastore;
import com.google.cloud.spanner.*;
import com.springml.nyc.taxi.ad.api.RedeemStatus;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by kaarthikraaj on 17/5/17.
 */
public class RedeemStoreManager {
    private static RedeemStoreManager redeemStoreMgr;
    private final String spannerInstanceId = "sml-coupon-redeem-store";
    private final String databaseName = "sml_coupon_redeem_store";
    private DatabaseClient client ;
    private final String tableName = "coupon_ride_ad";
    private static Spanner spanner;
    static
    {
        try {

            spanner = SpannerOptions.getDefaultInstance().getService();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private RedeemStoreManager(){

    }
    public synchronized static RedeemStoreManager getInstance(){
        if(redeemStoreMgr==null){
            redeemStoreMgr = new RedeemStoreManager();
        }
        return redeemStoreMgr;
    }

    private DatabaseClient getClient(){
        if(client==null) {
            DatabaseId id = DatabaseId.of(spanner.getOptions().getProjectId(), spannerInstanceId, databaseName);
            client = spanner.getDatabaseClient(id);
        }
        return client;
    }

    public boolean addCoupon(String rideId,String adId){
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
        }
        catch(SpannerException exc){
            //log
            response = false;
        }
        return response;
    }

    public RedeemStatus getRedeemStatus(String rideId, String adId){
        try (ReadOnlyTransaction readOnly = getClient().readOnlyTransaction()) { //readWriteTransaction()) {

            Struct couponredeemedResult = readOnly.readRow("coupon_ride_ad", Key.of(rideId, adId), Arrays.asList("availed"));

            if(couponredeemedResult==null) {
                return RedeemStatus.NONEXIST;
            }
            else {
                boolean redeemed = couponredeemedResult.getBoolean("availed");
                if(redeemed){
                    return RedeemStatus.REDEEMED;
                }
                else{
                    return RedeemStatus.NOTREDEEMED;
                }
            }
        }
        catch(SpannerException exc){
            //log
            return RedeemStatus.NOTREDEEMED;
        }
    }

    public boolean redeemCoupon(String rideId,String adId) {
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
                boolean isCouponredeemed = transaction.readRow("coupon_ride_ad", Key.of( rideId, adId), Arrays.asList("availed")).getBoolean("availed");
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
        else{
            response = false;
        }
        return response;
    }

    public boolean redeemCouponNonAtomic(String rideId,String adId) {
        boolean success = false;

        ReadOnlyTransaction readOnly = getClient().readOnlyTransaction();//readWriteTransaction();
        boolean isCouponredeemed = readOnly.readRow("coupon_ride_ad", Key.of( rideId, adId), Arrays.asList("availed")).getBoolean("availed");
        if (!isCouponredeemed){

            ArrayList<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(Mutation.newUpdateBuilder(tableName)
                    .set("ride_Id").to(rideId)
                    .set("ad_Id").to(adId)
                    .set("availed").to(true).build());
            getClient().write(mutations);
            success = true;
        }
        readOnly.close();
        // TransactionOptions.ReadWrite readWriteOption = TransactionOptions.getDefaultInstance().getReadWrite();

        return success;
    }

    public static void main(String s[]){
        RedeemStoreManager redeemStoreManager =  RedeemStoreManager.getInstance();
        redeemStoreManager.addCoupon("ride-1001","ad-101");
        boolean redeemed = redeemStoreManager.redeemCouponNonAtomic("ride-1001","ad-101");
        System.out.println("is coupon redeemed first call"+redeemed);
        redeemed = redeemStoreManager.redeemCouponNonAtomic("ride-1001","ad-101");
        System.out.println("is coupon redeemed second call"+redeemed);
        System.out.println( redeemStoreManager.getRedeemStatus("ride-1002","ad-101"));


        // System.exit(0);
    }
}
