package com.springml.coupon.service.simulator;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.cloud.dataflow.sdk.Pipeline;
import com.google.cloud.dataflow.sdk.coders.TableRowJsonCoder;
import com.google.cloud.dataflow.sdk.io.BigQueryIO;
import com.google.cloud.dataflow.sdk.io.PubsubIO;
import com.google.cloud.dataflow.sdk.options.PipelineOptionsFactory;
import com.google.cloud.dataflow.sdk.transforms.DoFn;
import com.google.cloud.dataflow.sdk.transforms.ParDo;
import com.google.cloud.dataflow.sdk.values.PCollection;
import com.google.gson.Gson;
import com.springml.coupon.service.simulator.model.Coupon;
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

    // Coupon JSON keys
    private static final String COUPON_ID = "coupon_id";
    private static final String DISCOUNT_PERCENTAGE = "discount_percentage";
    private static final String BUSINESS_TYPE = "business_type";
    private static final String BUSINESS_NAME = "business_name";
    private static final String COUPON_IMAGE = "coupon_image";
    // Ride Details JSON Key
    private static final String PASSENGER_COUNT = "passenger_count";
    private static final String TPEP_PICKUP_DATETIME = "tpep_pickup_datetime";
    private static final String PICKUP_LOCATION = "pickup_location";
    private static final String PICKUP_LATITUDE = "pickup_latitude";
    private static final String PICKUP_LONGITUDE = "pickup_longitude";
    private static final String DESTINATION_LOCATION = "destination_location";
    private static final String DROPOFF_LATITUDE = "dropoff_latitude";
    private static final String DROPOFF_LONGITUDE = "dropoff_longitude";
    private static final String STR_COMMA = ",";
    private static final String STR_EMPTY = "";
    public static final String APIKEY_HEADER = "apikey";

    private static class CouponServiceClient extends DoFn<TableRow, TableRow> {
        private String couponServiceUrl;
        private String apiKey;

        CouponServiceClient(String  couponServiceUrl, String apiKey) {
            this.couponServiceUrl = couponServiceUrl;
            this.apiKey = apiKey;
        }

        @Override
        public void processElement(ProcessContext c) throws Exception {
            TableRow tableRow = c.element();
            TableRow couponTableRow = getCoupon(tableRow);

            c.output(couponTableRow);
        }

        private TableRow getCoupon(TableRow tableRow) {
            try {
                HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
                HttpRequestFactory requestFactory = httpTransport.createRequestFactory(
                        httpRequest -> {
                            HttpHeaders headers = httpRequest.getHeaders();
                            headers.set(APIKEY_HEADER, apiKey);
                        });

                LOG.info("couponServiceUrl : " + couponServiceUrl);
                LOG.info("apiKey : " + apiKey);
                GenericUrl url = new GenericUrl(couponServiceUrl);

                JacksonFactory jacksonFactory = new JacksonFactory();
                JsonHttpContent jsonHttpContent = new JsonHttpContent(jacksonFactory, getPayLoad(tableRow));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                jsonHttpContent.writeTo(baos);
                LOG.info("Coupon Service Request Payload: " + baos.toString());
                HttpRequest request = requestFactory.buildPostRequest(url, jsonHttpContent);

                HttpResponse response = request.execute();
                String content = response.parseAsString();

                LOG.info("Coupon Service Response \n" + content);
                return getAsTableRow(content);
            } catch (Exception e) {
                LOG.error("Error while executing Coupon Service", e);
            }

            return getDefaultTableRow();
        }

        private TableRow getAsTableRow(String content) {
            Gson gson = new Gson();
            Coupon coupon = gson.fromJson(content, Coupon.class);

            TableRow tableRow = new TableRow();
            tableRow.set(COUPON_ID, coupon.getCouponId());
            tableRow.set(DISCOUNT_PERCENTAGE, coupon.getDiscountPercentage());
            tableRow.set(BUSINESS_TYPE, coupon.getBusinessType());
            tableRow.set(BUSINESS_NAME, coupon.getBusinessName());
            tableRow.set(COUPON_IMAGE, coupon.getCouponImage());

            return tableRow;
        }

        private TableRow getDefaultTableRow() {
            TableRow tableRow = new TableRow();
            tableRow.set(COUPON_ID, -1);
            tableRow.set(DISCOUNT_PERCENTAGE, -1);
            tableRow.set(BUSINESS_TYPE, STR_EMPTY);
            tableRow.set(BUSINESS_NAME, STR_EMPTY);
            tableRow.set(COUPON_IMAGE, STR_EMPTY);

            return tableRow;
        }

        private Map<String, Object> getPayLoad(TableRow tableRow) {
            Map<String, Object> map = new HashMap<>();

            int passengerCount = Integer.parseInt(tableRow.get(PASSENGER_COUNT).toString());
            map.put(PASSENGER_COUNT, passengerCount);

            String dropoffDatetime = tableRow.get(TPEP_PICKUP_DATETIME).toString();
            map.put(TPEP_PICKUP_DATETIME, dropoffDatetime);

            // Get the pickup latitude and longitude
            String pickupLocation = tableRow.get(PICKUP_LOCATION).toString();
            String[] latlon = pickupLocation.split(STR_COMMA);

            float pickupLon = Float.parseFloat(latlon[0]);
            map.put(PICKUP_LATITUDE, pickupLon);

            float pickupLat = Float.parseFloat(latlon[1]);
            map.put(PICKUP_LONGITUDE, pickupLat);

            // Get the dropoff latitude and longitude
            String destinationLocation = tableRow.get(DESTINATION_LOCATION).toString();
            String[] destLatLon = destinationLocation.split(STR_COMMA);

            float destLat = Float.parseFloat(destLatLon[0]);
            map.put(DROPOFF_LATITUDE, destLat);

            float destLon = Float.parseFloat(destLatLon[1]);
            map.put(DROPOFF_LONGITUDE, destLon);

            return map;
        }
    }

    private static TableSchema getCouponSchema() {
        List<TableFieldSchema> fields = new ArrayList<>();

        fields.add(new TableFieldSchema().setName(COUPON_ID).setType("INTEGER"));
        fields.add(new TableFieldSchema().setName(DISCOUNT_PERCENTAGE).setType("INTEGER"));
        fields.add(new TableFieldSchema().setName(BUSINESS_TYPE).setType("STRING"));
        fields.add(new TableFieldSchema().setName(BUSINESS_NAME).setType("STRING"));
        fields.add(new TableFieldSchema().setName(COUPON_IMAGE).setType("STRING"));

        return new TableSchema().setFields(fields);
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
        String apiKey = options.getApiKey();
        datastream.apply("Invoking Coupon Service", ParDo.of(new CouponServiceClient(couponServiceUrl, apiKey)))
            .apply(BigQueryIO.Write.named("Write to BigQuery")
                .withCreateDisposition(BigQueryIO.Write.CreateDisposition.CREATE_IF_NEEDED)
                .withSchema(getCouponSchema())
                .withWriteDisposition(BigQueryIO.Write.WriteDisposition.WRITE_APPEND)
                .to(options.getCouponTable()));

        p.run();
    }


}
