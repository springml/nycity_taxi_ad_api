package com.springml.nyc.taxi.coupon.redeem.server.api;

import com.springml.nyc.taxi.ad.datastore.RedeemStoreManager;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponDetails;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponRedeemStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by kaarthikraaj on 22/5/17.
 */
public class CouponRedeemServer {
    @Autowired
    RedeemStoreManager redeemStoreManager;

    public CouponRedeemStatusResponse redeemCoupon(CouponDetails couponRedeemRequestDetails){
        CouponRedeemStatusResponse response = new CouponRedeemStatusResponse();
        String rideId = couponRedeemRequestDetails.getRideId();
        String adId = couponRedeemRequestDetails.getAdId();
        response.setAdId(adId);
        response.setRideId(rideId);
        response.setRedeemed(redeemStoreManager.redeemCouponNonAtomic(rideId,adId));
        return response;
    }
}
