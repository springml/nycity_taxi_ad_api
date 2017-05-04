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

    @Override
    public String toString() {
        return "Coupon{" +
                "couponId=" + couponId +
                ", discountPercentage='" + discountPercentage + '\'' +
                '}';
    }
}
