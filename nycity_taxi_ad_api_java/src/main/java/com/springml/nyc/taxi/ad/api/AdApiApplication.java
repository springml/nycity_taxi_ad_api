package com.springml.nyc.taxi.ad.api;

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
 * Created by sam on 28/4/17.
 */
@SpringBootApplication
@EnableSwagger2
public class AdApiApplication {
    public static void main(String args[]) {
        SpringApplication.run(AdApiApplication.class, args);
    }

    @Bean
    public AdServer adServer() {
        return new AdServer();
    }

    @Bean
    public AdThresholdService adThresholdService() {
        return new AdThresholdService();
    }

    @Bean
    public Docket swaggerDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfo("Coupon Service", "Coupon Service", "1.0", null,
                        null, "Apache 2.0",
                        "http://www.apache.org/licenses/LICENSE-2.0"))
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.regex("^/(?!error|autoconfig|beans|configprops|dump|info|mappings|trace|env|metrics).*$"))
                .build();
    }
}
