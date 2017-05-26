package com.springml.nyc.taxi.coupon.redeem.server.api;

import com.springml.nyc.taxi.ad.api.RedeemStatus;
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
        String respMsg = "";
        response.setAdId(adId);
        response.setRideId(rideId);
        RedeemStatus status = redeemStoreManager.redeemCouponNonAtomic(rideId, adId);
        switch(status){
            case REDEEMED_SUCCESSFULLY:
                respMsg = "SuccessFully Redeemed";
                break;
            case ALREADY_REDEEMED:
                respMsg = "Failed -> AlreadyRedeemed";
                break;
            case NONEXIST:
                respMsg = "Failed -> Coupon doesnot exist";

        }
        response.setRedeemed(respMsg);
        return response;
    }
}
