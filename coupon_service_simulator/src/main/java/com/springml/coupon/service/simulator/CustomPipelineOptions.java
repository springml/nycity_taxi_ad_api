package com.springml.coupon.service.simulator;

import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.Validation;

/**
 * Options class for SML Simulator GDF
 */
public interface CustomPipelineOptions extends DataflowPipelineOptions {
    @Description("Redeem Coupon URl which will be used for coupon redemption")
    @Default.String("https://coupon-redeem-service-dot-billion-taxi-rides.appspot.com/redeemCoupon")
    @Validation.Required
    String getRedeemCouponServiceUrl();

    void setRedeemCouponServiceUrl(String value);

    @Description("ProjectId where data source topic lives")
    @Default.String("billion-taxi-rides")
    @Validation.Required
    String getSourceProject();

    void setSourceProject(String value);

    @Description("TopicId of source topic")
    @Default.String("smlfeed")
    @Validation.Required
    String getSourceTopic();

    void setSourceTopic(String value);

    @Description("Coupon Service REST URL")
    @Default.String("https://coupon-service-dot-billion-taxi-rides.appspot.com/getCoupon")
    @Validation.Required
    String getCouponServiceUrl();

    void setCouponServiceUrl(String value);

    @Description("Fully qualified coupon table name")
    @Default.String("billion-taxi-rides:advertising.coupon1")
    @Validation.Required
    String getCouponTable();

    void setCouponTable(String value);

    @Description("Fully qualified redeem coupon statistics table name")
    @Default.String("billion-taxi-rides:advertising.redeem_coupon_statistics")
    @Validation.Required
    String getRedeemCouponTable();

    void setRedeemCouponTable(String value);

    @Description("Apigee API Ket")
    @Default.String("lgvfwq6XmvzmuA7VwOLWY3VNOnMaI4ES")
    @Validation.Required
    String getApiKey();

    void setapiKey(String value);
}
