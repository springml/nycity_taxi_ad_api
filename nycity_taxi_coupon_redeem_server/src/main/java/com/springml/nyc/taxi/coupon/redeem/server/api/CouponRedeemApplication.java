package com.springml.nyc.taxi.coupon.redeem.server.api;
import com.springml.nyc.taxi.ad.datastore.RedeemStoreManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Spring Boot application's main class
 *
 */

@SpringBootApplication
@EnableSwagger2
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

    @Bean
    public Docket swaggerDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfo("Coupon Redeem Service", "Coupon Redeem Service", "1.0", null,
                        null, "Apache 2.0",
                        "http://www.apache.org/licenses/LICENSE-2.0"))
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }
}
