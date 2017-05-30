package com.springml.coupon.service.simulator.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

/**
 * Created by sam on 2/5/17.
 */
public class Coupon {
    @SerializedName("coupon_id")
    private int couponId;

    @SerializedName("ride_id")
    private String rideId;

    @SerializedName("discount_percentage")
    private int discountPercentage;

    @SerializedName("business_type")
    private String businessType;

    @SerializedName("business_name")
    private String businessName;

    @SerializedName("coupon_image")
    private String couponImage;

    public String getRideId() {
        return rideId;
    }

    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public int getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(int discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCouponImage() {
        return couponImage;
    }

    public void setCouponImage(String couponImage) {
        this.couponImage = couponImage;
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "couponId=" + couponId +
                ", discountPercentage=" + discountPercentage +
                ", businessType='" + businessType + '\'' +
                ", businessName='" + businessName + '\'' +
                ", couponImage='" + couponImage + '\'' +
                ", rideId='" + rideId +'\''+
                '}';
    }
}
