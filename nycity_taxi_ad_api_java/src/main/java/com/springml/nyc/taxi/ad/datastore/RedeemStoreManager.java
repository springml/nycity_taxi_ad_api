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
    private final String tableName = "Coupon_Ride_Ad_Redeem";
    private final String activity_Table_Name = "ReadWriteActivity";
    private final String index = "rideid_adid_availed_query_index";
    private static Spanner spanner;

    static {
        try {

            spanner = SpannerOptions.getDefaultInstance().getService();
           // System.out.println("Spanner initialised");
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
            System.out.println("database id is"+id.getDatabase()+id.getName()+id.getInstanceId());
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
    public boolean addCoupon(String rideId, String adId,String couponId) {
        boolean response = false;
        try {
            ArrayList<Mutation> mutations = new ArrayList<Mutation>();
            Mutation mutuation = Mutation.newInsertBuilder(tableName)
                    .set("ride_Id")
                    .to(rideId)
                    .set("ad_Id")
                    .to(adId)
                    .set("coupon_Id")
                    .to(couponId)
                    .set("availed")
                    .to(false)
                    .set("expired")
                    .to(false)
                    .build();
            mutations.add(mutuation);
            getClient().write(mutations);
           // addToActivity(rideId,adId,false);
            response = true;
        } catch (SpannerException exc) {
            LOG.error("Exception while adding coupon entry to redeem store" + exc.getMessage());
            response = false;
        }
        return response;
    }

  /*  private void addToActivity(String rideId, String adId, boolean isRead) {
        ArrayList<String> columns = new ArrayList<String>();
        Mutation.WriteBuilder builder = Mutation.newInsertOrUpdateBuilder(activity_Table_Name);
        columns.add("ReadCount");
        columns.add("WriteCount");
        Struct rs = getClient().readOnlyTransaction().readRow(activity_Table_Name,Key.of(rideId,adId), columns);
        long readcount = 0;
        long writeCount = 0;
        if(rs!=null) {
             readcount = rs.getLong("ReadCount");
             writeCount = rs.getLong("WriteCount");
            if (isRead) {
                readcount++;
            } else {
                writeCount++;
            }
        }
            Mutation updateMutation = builder.set("ride_Id").to(rideId)
                    .set("ad_Id").to(adId)
                    .set("ReadCount").to(readcount)
                    .set("WriteCount").to(writeCount).build();
            ArrayList<Mutation> mutations = new ArrayList<Mutation>();
            mutations.add(updateMutation);
            getClient().write(mutations);
        }

*/

    /*
    Gets coupon's redeem status
    @param  rideId Ride id that contains details like pickup,drop location,no of passengers etc
    @param adId Advertisement ID which is to be displayed in that ride
    The method is invoked while executing getCoupon service job after the count is retreived
    using big query
     */
    public RedeemStatus getRedeemStatus(String rideId, String adId) {
        try (ReadOnlyTransaction readOnly = getClient().readOnlyTransaction()) { //readWriteTransaction()) {
            RedeemStatus currentStatus = RedeemStatus.NOTREDEEMED;
           // addToActivity(rideId,adId,true);

            ResultSet redeemrs = readOnly.readUsingIndex(tableName,index,KeySet.singleKey(Key.of(rideId,adId,true,false)),Arrays.asList("availed"));
            if(redeemrs.next()){
                currentStatus = RedeemStatus.REDEEMED;
                redeemrs.close();
            }
            else {

                ResultSet notRedeemedRs = readOnly.readUsingIndex(tableName, index, KeySet.singleKey(Key.of(rideId, adId, false,false)), Arrays.asList("availed"));
                boolean isDataPresent = notRedeemedRs.next();
                if(isDataPresent){
                    currentStatus = RedeemStatus.NOTREDEEMED;
                }
                else{
                    currentStatus = RedeemStatus.NONEXIST;
                }
                notRedeemedRs.close();
            }
            return currentStatus;


            /*Struct couponredeemedResult = readOnly.readRow("coupon_ride_ad", Key.of(rideId, adId), Arrays.asList("availed"));

            if (couponredeemedResult == null) {
                return RedeemStatus.NONEXIST;
            } else {
                boolean redeemed = couponredeemedResult.getBoolean("availed");
                if (redeemed) {
                    return RedeemStatus.REDEEMED;
                } else {
                    return RedeemStatus.NOTREDEEMED;
                }
            }*/
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
    public void addCouponTransactionally(String rideId, String adId,String couponId) {
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
                /*
                boolean isCouponredeemed = transaction.readRow("coupon_ride_ad", Key.of(rideId, adId), Arrays.asList("availed")).getBoolean("availed");
                if (!isCouponredeemed) {
                    transaction.buffer(Mutation.newUpdateBuilder(tableName)
                            .set("ride_Id").to(rideId)
                            .set("ad_Id").to(adId)
                            .set("availed").to(true).build());
                }
            */
                RedeemStatus currentStatus = RedeemStatus.NOTREDEEMED;

                ArrayList<Mutation> transactions = new ArrayList<Mutation>();
                ResultSet redeemrs = transaction.readUsingIndex(tableName,index,KeySet.singleKey(Key.of(rideId,adId,true,false)),Arrays.asList("availed"));
               // addToActivity(rideId,adId,true);

                if(redeemrs.next()){
                    currentStatus = RedeemStatus.REDEEMED;
                    redeemrs.close();
                }
                else {

                    ResultSet notRedeemedRs = transaction.readUsingIndex(tableName, index, KeySet.singleKey(Key.of(rideId, adId, false,false)), Arrays.asList("availed"));
                   // addToActivity(rideId,adId,true);

                    boolean isDataPresent = notRedeemedRs.next();
                    if(isDataPresent){
                        currentStatus = RedeemStatus.NOTREDEEMED;
                    }
                    else{
                        currentStatus = RedeemStatus.NONEXIST;
                    }
                    notRedeemedRs.close();
                }


                if(currentStatus.equals(RedeemStatus.NOTREDEEMED)){
                    // Statement.newBuilder("select column_Id from "+tableName+" where ")
                    // transaction.executeQuery()
                    addQueryToExpireCoupon(rideId,adId,transaction,transactions);
                    addNewCoupon(rideId,adId,couponId,transaction,transactions);

                }
                else {
                    if(currentStatus.equals(RedeemStatus.NONEXIST)){
                        addNewCoupon(rideId,adId,couponId,transaction,transactions);
                    }
                    else {
                        if (currentStatus.equals(RedeemStatus.REDEEMED)) {
                            return null;
                        }
                    }
                }


                transaction.buffer(transactions);

                return null;
            }

        });

    }

    private void addNewCoupon(String rideId,String adId,String couponId,TransactionContext transaction,ArrayList<Mutation> transactions) {
        transactions.add(Mutation.newInsertBuilder(tableName)
                .set("ride_Id")
                .to(rideId)
                .set("ad_Id")
                .to(adId)
                .set("coupon_Id")
                .to(couponId)
                .set("availed")
                .to(false)
                .set("expired")
                .to(false)
                .build());
       // addToActivity(rideId,adId,false);

    }

    private void addQueryToExpireCoupon(String rideId,String adId,TransactionContext transaction,ArrayList<Mutation> transactions) {
        ResultSet couponIdRs = transaction.readUsingIndex(tableName,index,KeySet.singleKey(Key.of(rideId,adId,false,false)),Arrays.asList("coupon_Id"));
        couponIdRs.next();
        String couponId = couponIdRs.getString("coupon_Id");
        System.out.println("Coupon received is "+couponId);
        couponIdRs.close();
        transactions.add(Mutation.newUpdateBuilder(tableName)
                .set("ride_Id").to(rideId)
                .set("ad_Id").to(adId)
                .set("coupon_Id").to(couponId)
                .set("availed").to(false)
                .set("expired").to(true)
                .build());
       // addToActivity(rideId,adId,false);

    }


    /*
    Redeems the coupon
    @param  rideId Ride id that contains details like pickup,drop location,no of passengers etc
    @param adId Advertisement ID which is to be displayed in that ride
    The method is invoked by redeemCoupon service
     */
    public RedeemStatus redeemCouponNonAtomic(String rideId, String adId,String couponId) {
        RedeemStatus status = RedeemStatus.NONEXIST;
        try {
            ReadOnlyTransaction readOnly = getClient().readOnlyTransaction();//readWriteTransaction();
            Struct isCouponredeemedStruct = readOnly.readRow(tableName, Key.of(rideId, adId, couponId), Arrays.asList("availed"));
            if(isCouponredeemedStruct!=null) {

                boolean  isCouponRedeemed = isCouponredeemedStruct.getBoolean("availed");
                if (!isCouponRedeemed) {

                    ArrayList<Mutation> mutations = new ArrayList<Mutation>();
                    mutations.add(Mutation.newUpdateBuilder(tableName)
                            .set("ride_Id").to(rideId)
                            .set("ad_Id").to(adId)
                            .set("coupon_Id").to(couponId)
                            .set("availed").to(true).build());
                    getClient().write(mutations);
                    status = RedeemStatus.REDEEMED_SUCCESSFULLY;
                } else {
                    status = RedeemStatus.ALREADYREDEEMED;
                }
            }
            else{
                status = RedeemStatus.NONEXIST;
            }
            readOnly.close();
            LOG.info("coupon redemption status? " + status);

        }
        catch(SpannerException exc){
            LOG.info("Exception wile redeeming"+exc.getMessage());
        }
        return status;
    }

    public static void main(String s[]) {
        RedeemStoreManager redeemStoreManager = RedeemStoreManager.getInstance();
        // redeemStoreManager.addCoupon("ride-1001", "ad-101","1235");
        // redeemStoreManager.addCouponTransactionally("ride-1013", "ad-101","1234");
        // redeemStoreManager.addCouponTransactionally("ride-1013", "ad-101","1235");
        // redeemStoreManager.addCouponTransactionally("ride-1013", "ad-101","1236");

        // System.out.println("first time"+ redeemStoreManager.redeemCouponNonAtomic("ride-1001", "ad-101","1234"));
        // System.out.println("second time"+ redeemStoreManager.redeemCouponNonAtomic("ride-1001", "ad-101","1234"));

        //  redeemStoreManager.addCoupon("ride-1001", "ad-101","1234");

       // RedeemStatus redeemedStatus = redeemStoreManager.redeemCouponNonAtomic("RideDetails{passengerCount=3, tpepPickupDatetime='2015-01-04 20:00:03', pickupLatitude='40.724983', pickupLongitude='-73.99567', dropoffLatitude='40.76858', dropoffLongitude='-73.86199'}", "7","coupon-21fb989b-7fa7-4f1e-a73c-9a2cf55ece45");
       // System.out.println("is coupon redeemed first call" + redeemedStatus);
      //  redeemedStatus = redeemStoreManager.redeemCouponNonAtomic("RideDetails{passengerCount=2, tpepPickupDatetime='2015-01-04 20:00:03', pickupLatitude='40.724983', pickupLongitude='-73.99567', dropoffLatitude='40.76858', dropoffLongitude='-73.86199'}", "7","coupon-21fb989b-7fa7-4f1e-a73c-9a2cf55ece45");
       // System.out.println("is coupon redeemed second call" + redeemedStatus);
         System.out.println(redeemStoreManager.getRedeemStatus("ride-1002", "ad-101"));


        // System.exit(0);
    }
}
