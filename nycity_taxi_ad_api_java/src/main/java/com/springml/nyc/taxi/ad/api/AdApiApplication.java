package com.springml.nyc.taxi.ad.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Created by sam on 28/4/17.
 */
@SpringBootApplication
public class AdApiApplication {
    public static void main(String args[]) {
        SpringApplication.run(AdApiApplication.class, args);
    }

    @Bean
    public AdServer adServer() {
        return new AdServer();
    }
}
