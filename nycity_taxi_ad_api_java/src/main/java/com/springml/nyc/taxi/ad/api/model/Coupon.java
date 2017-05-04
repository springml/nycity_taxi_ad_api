package com.springml.nyc.taxi.ad.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by sam on 2/5/17.
 */
public class Coupon {
    @JsonProperty("coupon_id")
    private int couponId;

    @JsonProperty("discount_percentage")
    private int discountPercentage;

    @JsonProperty("business_type")
    private String businessType;

    @JsonProperty("business_name")
    private String businessName;

    @JsonProperty("coupon_image")
    private String couponImage;

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
                '}';
    }
}
