# nycity_taxi_ad_api
Taxi demo with Ad API

## Introduction

This document contains the steps required to configure, build and deploy coupon service (Ad Service) into GAE

## Prerequisite

1. Make sure Java 8 is installed and JAVA_HOME is set in environment

2. Make sure maven is installed and added in PATH

3. Make sure gcloud is installed and project is set to **"****billion-taxi-rides"**

## Coupon Service Setup

1. Open terminal or command prompt and execute "**git clone ****[https://github.com/springml/nycity_taxi_ad_ap**i](https://github.com/springml/nycity_taxi_ad_api)"

2. **cd nycity_taxi_ad_api/nycity_taxi_ad_api_java**

3. To build coupon service execute "**mvn clean package**"

4. To run coupon service locally execute "**java -jar target/nycity-taxi-ad-api-1.0.0.jar**"

5. To deploy coupon service into GAE execute "**mvn appengine:deploy**". Rest service can be accessed using https://coupon-service-dot-billion-taxi-rides.appspot.com/getCoupon

### Configuration

1. By default service is set to **coupon-service. **You can change it in /nycity_taxi_ad_api/nycity_taxi_ad_api_java/src/main/appengine/app.yaml

2. Additional configurations related to GAE environment has to be done in /nycity_taxi_ad_api/nycity_taxi_ad_api_java/src/main/appengine/app.yaml

3. By default coupon-service uses CloudML from **billion-taxi-rides. **This can be changed by providing the CloudML REST url in **cloudML.predict.rest.url **property in /nycity_taxi_ad_api/nycity_taxi_ad_api_java/src/main/resources/application.properties

4. For any changes in coupon discount mapping, edit the file present in /nycity_taxi_ad_api/nycity_taxi_ad_api_java/src/main/resources/coupon-discount.properties

**Note:- **Any changes in configuration will be reflected only when the app is rebuilt and redeployed in to GAE.

### Invoke Coupon Service

1. Install and open any REST client like Postman or Advanced REST Client…

2. Use the POST method 

3. Use the URL [https://coupon-service-dot-billion-taxi-rides.appspot.com/getCoupon](https://coupon-service-dot-billion-taxi-rides.appspot.com/getCoupon). Please make sure to change the service is same as you’ve provided in app.yaml

4. Add the "Content-Type" header with “application/json” as value

5. Use the below json content as body

*{*

*  "passenger_count":2,*

*  "tpep_pickup_datetime":"2015-01-04 20:06:55",*

*  "pickup_latitude":"40.751056671142578",*

*  "pickup_longitude":"-73.994209289550781",*

*  "dropoff_latitude":"40.749996185302734",*

*  "dropoff_longitude":"-73.97900390625"*

*}*

6. On executing the above request you will get a response like below

*{*

*  "couponId": 9,*

*  "discount": "12%"*

*}*

![image alt text](image_0.png)

## Run Simulator

### Injector

1. Login into Google Cloud Console and select Google Compute Engine from the menu

2. Click on the "SSH" dropdown adjacent to “[coupon-service-injector](https://console.cloud.google.com/compute/instancesDetail/zones/us-central1-a/instances/coupon-service-injector?project=billion-taxi-rides)” VM and click “Open in browser window”

3. In terminal switch to root user using *sudo su -*

4. *cd nycity_taxi_ad_api/injector/*

5. Edit config_without_docker.sh if you need to increase the speed or  change any other configuration

6. Execute *source config_without_docker.sh*

7. Execute *go run *.go*

### Dataflow

1. Login into Google Cloud Console and select Google Compute Engine from the menu

2. Click on the "SSH" dropdown adjacent to “ [coupon-service-simulator](https://console.cloud.google.com/compute/instancesDetail/zones/us-central1-a/instances/coupon-service-simulator?project=billion-taxi-rides)” VM and click “Open in browser window”

3. In terminal switch to root user using *sudo su -*

4. *cd dataflow/nycity_taxi_ad_api/coupon_service_simulator/*

5. Execute *mvn clean compile exec:java -Dexec.mainClass=com.springml.coupon.service.simulator.CouponServiceGDF -e -Dexec.args="--project=billion-taxi-rides --stagingLocation=gs://sml/staging --runner=DataflowPipelineRunner --streaming=true --numWorkers=20 --zone=us-west1-b --couponServiceUrl=https://demo5-test.apigee.net/v1/coupon --apiKey=API_KEY"*

### Scheduler

1. Login into Google Cloud Console and select Google Compute Engine from the menu

2. Click on the "SSH" dropdown adjacent to “ [coupon-service-simulator](https://console.cloud.google.com/compute/instancesDetail/zones/us-central1-a/instances/coupon-service-simulator?project=billion-taxi-rides)” VM and click “Open in browser window”

3. In terminal switch to root user using *sudo su -*

4. *cd scheduler-demo/nycity_taxi_ad_api/nycity_taxi_ad_scheduler/*

5. Execute *java -jar target/nycity-taxi-ad-scheduler-1.0-SNAPSHOT.jar*

## Run Taxi Dataflow which will publish data into BigQuery
We have to start injector and dataflow.

### Dataflow
To start dataflow follow below steps

1. Start sml-tax-dataflow compute-engine and ssh into it

2. sudo su -

3. cd work/dataflow_nyc_taxis_next17_visupipe/dataflow/

4. mvn clean compile exec:java -Dexec.mainClass=com.springml.TaxiGDF -e -Dexec.args="--project=billion-taxi-rides   --sinkProject=billion-taxi-rides --stagingLocation=gs://billion-taxi-rides/df-staging/ --runner=DataflowPipelineRunner --streaming=true --numWorkers=1 --zone=us-west1-b"

5. Check whether the stop successfully started by navigating to https://console.cloud.google.com/dataflow?project=billion-taxi-rides

### Injector
To start injector follow below steps

1. Start next17keynote-injector compute-engine and ssh into it

2. sudo su -

3. cd nycity_taxi_ad_api/injector/

4. source config_without_docker.bash

5. go run *.go