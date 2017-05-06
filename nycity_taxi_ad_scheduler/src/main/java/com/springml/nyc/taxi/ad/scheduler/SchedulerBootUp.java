package com.springml.nyc.taxi.ad.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * This is the Main Entry class to start the scheduler job
 * Run this class to start the AdCountUpdater scheduler Job
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan({"com.springml.nyc.taxi.ad.scheduler"})
public class SchedulerBootUp {
    public static void main(String[] args) {

        SpringApplication.run(new Object[]{SchedulerBootUp.class}, args);

    }
}
