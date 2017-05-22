package com.springml.nyc.taxi.coupon.redeem.server.api;
import com.springml.nyc.taxi.ad.datastore.RedeemStoreManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
/**
 * Created by kaarthikraaj on 22/5/17.
 */

@SpringBootApplication
public class CouponRedeemApplication {
    public static void main(String args[]) {
        SpringApplication.run(CouponRedeemApplication.class, args);
    }

    @Bean
    public CouponRedeemServer couponRedeemServer() {
        return new CouponRedeemServer();
    }

    @Bean
    public RedeemStoreManager redeemStoreManager() {
        return RedeemStoreManager.getInstance();
    }
}
