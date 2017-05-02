package com.springml.nyc.taxi.ad.api.model;

/**
 * Created by sam on 2/5/17.
 */
public class CouponResponse {
    private int couponId;
    private String discount;

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public String getDiscount() {
        return discount;
    }

    public void setDiscount(String discount) {
        this.discount = discount;
    }

    @Override
    public String toString() {
        return "CouponResponse{" +
                "couponId=" + couponId +
                ", discount='" + discount + '\'' +
                '}';
    }
}
