package com.springml.coupon.service.simulator;

import com.google.cloud.dataflow.sdk.options.DataflowPipelineOptions;
import com.google.cloud.dataflow.sdk.options.Default;
import com.google.cloud.dataflow.sdk.options.Description;
import com.google.cloud.dataflow.sdk.options.Validation;

/**
 * Options class for SML Simulator GDF
 */
public interface CustomPipelineOptions extends DataflowPipelineOptions {
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
}
