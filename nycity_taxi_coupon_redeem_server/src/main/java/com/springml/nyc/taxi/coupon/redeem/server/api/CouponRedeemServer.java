package com.springml.nyc.taxi.coupon.redeem.server.api;

import com.springml.nyc.taxi.ad.datastore.RedeemStoreManager;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponDetails;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponRedeemStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Coupon redeem server that has main api that gets invoked
 * coupon redeem action is performed
 */
public class CouponRedeemServer {
    @Autowired
    RedeemStoreManager redeemStoreManager;

    /* Responsible for redeeming coupon
       @param couponRedeemRequestDetails redeem coupon request representing json
        @return  CouponRedeemStatusResponse which will be converted to json
     */
    public CouponRedeemStatusResponse redeemCoupon(CouponDetails couponRedeemRequestDetails) {
        CouponRedeemStatusResponse response = new CouponRedeemStatusResponse();
        String rideId = couponRedeemRequestDetails.getRideId();
        String adId = couponRedeemRequestDetails.getAdId();
        String couponId = couponRedeemRequestDetails.getCouponId();
        response.setAdId(adId);
        response.setRideId(rideId);
        boolean redeemed = redeemStoreManager.redeemCouponNonAtomic(rideId, adId,couponId);
        response.setRedeemed(redeemed?"Successfully Redeemed":"Redeem Failed: Coupon Already Redeemed");
        return response;
    }
}