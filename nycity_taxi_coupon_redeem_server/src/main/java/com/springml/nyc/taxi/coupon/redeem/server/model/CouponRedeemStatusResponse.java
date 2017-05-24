package com.springml.nyc.taxi.coupon.redeem.server.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents Coupon redemption service response json
 */
public class CouponRedeemStatusResponse {
    @JsonProperty("ride_id")
    private String rideId;

    @JsonProperty("ad_id")
    private String adId;

    @JsonProperty("redeemed")
    private String redeemedStatus;

    @JsonProperty("coupon_id")
    private String couponId;

    public String getCouponId() {
        return couponId;
    }

    public void setCouponId(String couponId) {
        this.couponId = couponId;
    }

    public String isRedeemed() {
        return redeemedStatus;
    }

    public void setRedeemed(String redeemedStatus) {
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
                ", ad_id='" + adId  +
                ", coupon_id='" + couponId  +

                ", redeemed='" + redeemedStatus + '\'' +

                '}';
    }
}
