package com.springml.nyc.taxi.ad.scheduler;

import com.google.cloud.WaitForOption;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.QueryResponse;
import com.google.cloud.bigquery.QueryRequest;
import com.google.cloud.bigquery.QueryResult;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.FieldValue;


import com.springml.nyc.taxi.ad.datastore.AdCountStoreManager;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * The scheduler job that runs every configured that gets ad count
 * for every ad for the day from bigquery and stores them in datastore
 */
@Component
public class AdCountUpdaterJob {
    private Logger logger =
            Logger.getLogger(AdCountUpdaterJob.class.getName());

    /*configure here the frequency in millisecs this job has to run
    This is the method that gets invoked as scheduler job
    */
    @Scheduled(fixedRateString = "${fixed.delay.seconds}000")
    public void updateAdCounts() {
        //gets adcount results from big query
        Properties adCountResponseObjects = getAdvertisementDisplayCount();
        //stores them in AdCountStore(DataStore)
        AdCountStoreManager.getInstance().updateAdCountEntity(adCountResponseObjects);
    }

    public static void main(String args[]) {
        AdCountUpdaterJob updater = new AdCountUpdaterJob();
        updater.updateAdCounts();
        int count = AdCountStoreManager.getInstance().getAdUpdateCount(Integer.toString(9));
        System.out.println("The count for adId 9 is " + count);
    }

    /*
        Prepares for authorization to build BigQuery Client
    */
    private BigQuery createAuthorizedClient() {
        BigQuery bigQueryClient = null;

        try {
            bigQueryClient = BigQueryOptions.getDefaultInstance().getService();
        } catch (Exception exception) {
            logger.severe("Exception while preparing for authorization :" + exception.getMessage());
        }

        return bigQueryClient;
    }

    /* Runs bigquery job to get adcount for all advertisements
       and prepares result as properties with ad ID as key and
       displaycount as value
     */
    private Properties getAdvertisementDisplayCount() {
        Properties adDisplayCountResponses = new Properties();

        BigQuery bigQueryClient = createAuthorizedClient();

        String fetchAdDisCountQuery = generateAdDiscountQuery();
        runAdCountQueryAsJob(fetchAdDisCountQuery, bigQueryClient, adDisplayCountResponses);

        return adDisplayCountResponses;
    }

    /*
       Runs AdCount retreival query as bigquery job
     */
    private void runAdCountQueryAsJob(String fetchAdDisCountQuery, BigQuery bigQueryClient, Properties adDisplayCountResponses) {

        Job adDisplayCountJob = createAdDisplayCountJob(fetchAdDisCountQuery, bigQueryClient);
        waitForJobCompletion(adDisplayCountJob);
        QueryResponse response = bigQueryClient.getQueryResults(adDisplayCountJob.getJobId());
        processResponse(response, adDisplayCountResponses);
    }

    /*
    Handles response after query execution.
    Process errors if any.
    If there is no error,it will populate result properies
     */
    private void processResponse(QueryResponse response, Properties adDisplayCountResponses) {

        List<BigQueryError> executionErrors = response.getExecutionErrors();
        // look for errors in executionErrors
        if (executionErrors.isEmpty()) {
            QueryResult result = response.getResult();
            populateAdDisplayCountResponse(result, adDisplayCountResponses);
            logger.info("Advertisement DisplayCount scheduler fetched adCount successfully");
        } else {
            StringBuilder errorMsg = new StringBuilder();
            for (BigQueryError err : executionErrors) {
                errorMsg.append(err.getMessage()).append('\n');
            }
            logger.severe("Error while executing bigquery job" + errorMsg.toString());
        }
    }

    /*
           Runs AdCount retreival query without Big query job
         */
    private void runAdCountQueryWithoutJob(String fetchAdDisCountQuery, BigQuery bigQueryClient, Properties adDisplayCountResponses) {

        QueryRequest request = QueryRequest.newBuilder(fetchAdDisCountQuery).build();

        QueryResponse response = bigQueryClient.query(request);
        while (!response.jobCompleted()) {
            try {
                Thread.sleep(1000);
                response = bigQueryClient.getQueryResults(response.getJobId());
            } catch (InterruptedException ie) {
                logger.warning("exception while waiting for the completion of adcount big query execution");
            }
        }
        processResponse(response, adDisplayCountResponses);
    }

    /*
    Creates big query Job for running query for adCount retreival
     */
    private Job createAdDisplayCountJob(String fetchAdDisCountQuery, BigQuery bigQueryClient) {
        QueryJobConfiguration.Builder queryJobBuilder = QueryJobConfiguration.newBuilder(fetchAdDisCountQuery);
        QueryJobConfiguration jobConfig = queryJobBuilder.build();

        JobInfo.Builder builder = Job.newBuilder(jobConfig);
        JobInfo adDisplayCountJobInfo = builder.build();
        Job adDisplayCountJob = bigQueryClient.create(adDisplayCountJobInfo);
        return adDisplayCountJob;
    }

    /*
    Generates AdDisplayCount query
     */
    private String generateAdDiscountQuery() {
        String fetchAdDisCountQuery = "SELECT campaign,count(*) as AdCount from (SELECT  campaign,ride_id,  \n" +
                "FROM [billion-taxi-rides:advertising.taxi_rides]\n" +
                "WHERE TIMESTAMP(timestamp) >= TIMESTAMP(CURRENT_DATE())\n" +
                "GROUP BY campaign,ride_id)\n" +
                "GROUP BY campaign";
        return fetchAdDisCountQuery;
    }

    private void waitForJobCompletion(Job adDisplayCountJob) {
        try {
            adDisplayCountJob.waitFor(WaitForOption.checkEvery(1, TimeUnit.SECONDS));

        } catch (InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    /*
    Process result after query execution and prepares response properties
    with adIds as keys and their count as Values
     */
    private void populateAdDisplayCountResponse(QueryResult result, Properties adDisplayCountResponse) {

        Iterator<List<FieldValue>> rowItr = result.iterateAll().iterator();

        while (rowItr.hasNext()) {
            List<FieldValue> columns = rowItr.next();
            String key = "";
            String value = "";
            for (int loopCount = 0; loopCount < columns.size(); loopCount++) {
                if (loopCount % 2 == 0) {
                    key = columns.get(loopCount).getStringValue();
                } else {
                    value = columns.get(loopCount).getStringValue();
                }
            }
            adDisplayCountResponse.setProperty(key, value);
        }

    }
}
