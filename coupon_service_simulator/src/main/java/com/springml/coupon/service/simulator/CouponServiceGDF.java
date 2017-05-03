package com.springml.coupon.service.simulator;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.model.TableRow;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.coders.TableRowJsonCoder;
import com.google.cloud.dataflow.sdk.io.PubsubIO;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.google.gson.Gson;
import com.springml.coupon.service.simulator.model.CouponResponse;
import com.springml.coupon.service.simulator.model.RideDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sam on 3/5/17.
 */
public class CouponServiceGDF {
    private static final Logger LOG = LoggerFactory.getLogger(CouponServiceGDF.class);

    private static class CouponServiceClient extends DoFn<TableRow, TableRow> {
        private String couponServiceUrl;

        CouponServiceClient(String  couponServiceUrl) {
            this.couponServiceUrl = couponServiceUrl;
        }

        @Override
        public void processElement(ProcessContext c) throws Exception {
            TableRow tableRow = c.element();
            TableRow resultantTableRow = invokeCloudML(tableRow);

            c.output(resultantTableRow);
        }

        private TableRow invokeCloudML(TableRow tableRow) {
            try {
                HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                        httpRequest -> {
                            httpRequest.setConnectTimeout(0);
                            httpRequest.setReadTimeout(0);
                        });

                GenericUrl url = new GenericUrl(couponServiceUrl);

                JacksonFactory jacksonFactory = new JacksonFactory();
                JsonHttpContent jsonHttpContent = new JsonHttpContent(jacksonFactory, getPayLoad(tableRow));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                jsonHttpContent.writeTo(baos);
                LOG.info("Executing request... " + baos.toString());
                HttpRequest request = requestFactory.buildPostRequest(url, jsonHttpContent);

                HttpResponse response = request.execute();
                String content = response.parseAsString();

                LOG.info("Got the following response from CloudML \n" + content);
                return getAsTableRow(content);
            } catch (Exception e) {
                LOG.error("Error while executing CloudML", e);
                return getAsTableRow(-1, "Error while executing CloudML");
            }
        }

        private TableRow getAsTableRow(String content) {
            Gson gson = new Gson();
            CouponResponse couponResponse = gson.fromJson(content, CouponResponse.class);

            return getAsTableRow(couponResponse.getCouponId(), couponResponse.getDiscount());
        }

        private TableRow getAsTableRow(int couponId, String discount) {
            TableRow tableRow = new TableRow();
            tableRow.set("couponId", couponId);
            tableRow.set("discount", discount);

            return tableRow;
        }

//        private RideDetails getPayLoad(TableRow tableRow) {
//            RideDetails rideDetails = new RideDetails();
//
//            int passengerCount = Integer.parseInt(tableRow.get("passenger_count").toString());
//            rideDetails.setPassengerCount(passengerCount);
//
//            String pickupDatetime = tableRow.get("tpep_pickup_datetime").toString();
//            rideDetails.setTpepPickupDatetime(pickupDatetime);
//
//            // Get the pickup latitude and longitude
//            String pickupLocation = tableRow.get("pickup_location").toString();
//            String[] latlon = pickupLocation.split(",");
//
//            float pickupLat = Float.parseFloat(latlon[0]);
//            rideDetails.setPickupLatitude(pickupLat);
//
//            float pickupLon = Float.parseFloat(latlon[1]);
//            rideDetails.setPickupLongitude(pickupLon);
//
//            // Get the dropoff latitude and longitude
//            String destinationLocation = tableRow.get("destination_location").toString();
//            String[] destLatLon = destinationLocation.split(",");
//
//            float destLat = Float.parseFloat(destLatLon[0]);
//            rideDetails.setDropoffLatitude(destLat);
//
//            float destLon = Float.parseFloat(destLatLon[1]);
//            rideDetails.setDropoffLatitude(destLon);
//
//            return rideDetails;
//        }

        private Map<String, Object> getPayLoad(TableRow tableRow) {
            Map<String, Object> map = new HashMap<>();

            int passengerCount = Integer.parseInt(tableRow.get("passenger_count").toString());
            map.put("passenger_count", passengerCount);

            String dropoffDatetime = tableRow.get("tpep_pickup_datetime").toString();
            map.put("tpep_pickup_datetime", dropoffDatetime);

            // Get the pickup latitude and longitude
            String pickupLocation = tableRow.get("pickup_location").toString();
            String[] latlon = pickupLocation.split(",");

            float pickupLon = Float.parseFloat(latlon[0]);
            map.put("pickup_latitude", pickupLon);

            float pickupLat = Float.parseFloat(latlon[1]);
            map.put("pickup_longitude", pickupLat);

            // Get the dropoff latitude and longitude
            String destinationLocation = tableRow.get("destination_location").toString();
            String[] destLatLon = destinationLocation.split(",");

            float destLat = Float.parseFloat(destLatLon[0]);
            map.put("dropoff_latitude", destLat);

            float destLon = Float.parseFloat(destLatLon[1]);
            map.put("dropoff_longitude", destLon);

            return map;
        }
    }

    public static void main(String args[]) {
        CustomPipelineOptions options =
                PipelineOptionsFactory.fromArgs(args).withValidation().as(CustomPipelineOptions.class);
        Pipeline p = Pipeline.create(options);

        PCollection<TableRow> datastream = p.apply(PubsubIO.Read.named("Read rides from PubSub")
                .topic(String.format("projects/%s/topics/%s", options.getSourceProject(), options.getSourceTopic()))
                .timestampLabel("ts")
                .withCoder(TableRowJsonCoder.of()));

        String couponServiceUrl = options.getCouponServiceUrl();
        datastream.apply("Invoking Coupon Service", ParDo.of(new CouponServiceClient(couponServiceUrl)));
        p.run();
    }


}
