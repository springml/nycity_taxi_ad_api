package com.springml.nyc.taxi.coupon.redeem.server.api;

import com.springml.nyc.taxi.ad.datastore.RedeemStoreManager;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponDetails;
import com.springml.nyc.taxi.coupon.redeem.server.model.CouponRedeemStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
/**
 * Created by kaarthikraaj on 22/5/17.
 */

@RestController
public class CouponRedeemResource {
    @Autowired
    CouponRedeemServer couponRedeemServer;

    @RequestMapping("/")
    public String home() {
        return "Coupon Redeem Service is running!";
    }

    @RequestMapping(value = "/redeemCoupon",method = RequestMethod.POST)
    public @ResponseBody ResponseEntity<CouponRedeemStatusResponse> redeemCoupon(@RequestBody CouponDetails redeemCouponRequest) {
        return ResponseEntity.ok( couponRedeemServer.redeemCoupon(redeemCouponRequest));
    }

}
