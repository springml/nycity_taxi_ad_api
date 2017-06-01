package com.springml.coupon.service.simulator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Represents Coupon redemption service response json
 */
public class CouponRedeemStatusResponse {
    @SerializedName("ride_id")
    private String rideId;

    @SerializedName("ad_id")
    private String adId;

    @SerializedName("redeemedStatus")
    private String redeemedStatus;

    public String getRedeemedStatus() {
        return redeemedStatus;
    }

    public void setRedeemedStatus(String redeemedStatus) {
        this.redeemedStatus = redeemedStatus;
    }

    public String getRideId() {
        return rideId;
    }

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    @Override
    public String toString() {
        return "coupon_Details{" +
                "ride_id=" + rideId +
                ", ad_id='" + adId + '\'' +
                ", redeemed='" + redeemedStatus + '\'' +

                '}';
    }
}
