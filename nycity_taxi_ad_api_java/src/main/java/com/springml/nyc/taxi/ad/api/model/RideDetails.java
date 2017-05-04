package com.springml.nyc.taxi.ad.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by sam on 28/4/17.
 */
public class RideDetails {
    @JsonProperty("passenger_count")
    private int passengerCount;

    @JsonProperty("tpep_pickup_datetime")
    private String tpepPickupDatetime;

    @JsonProperty("pickup_latitude")
    private float pickupLatitude;

    @JsonProperty("pickup_longitude")
    private float pickupLongitude;

    @JsonProperty("dropoff_latitude")
    private float dropoffLatitude;

    @JsonProperty("dropoff_longitude")
    private float dropoffLongitude;

    public int getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(int passengerCount) {
        this.passengerCount = passengerCount;
    }

    public String getTpepPickupDatetime() {
        return tpepPickupDatetime;
    }

    public void setTpepPickupDatetime(String tpepPickupDatetime) {
        this.tpepPickupDatetime = tpepPickupDatetime;
    }

    public float getPickupLatitude() {
        return pickupLatitude;
    }

    public void setPickupLatitude(float pickupLatitude) {
        this.pickupLatitude = pickupLatitude;
    }

    public float getPickupLongitude() {
        return pickupLongitude;
    }

    public void setPickupLongitude(float pickupLongitude) {
        this.pickupLongitude = pickupLongitude;
    }

    public float getDropoffLatitude() {
        return dropoffLatitude;
    }

    public void setDropoffLatitude(float dropoffLatitude) {
        this.dropoffLatitude = dropoffLatitude;
    }

    public float getDropoffLongitude() {
        return dropoffLongitude;
    }

    public void setDropoffLongitude(float dropoffLongitude) {
        this.dropoffLongitude = dropoffLongitude;
    }

    @Override
    public String toString() {
        return "RideDetails{" +
                "passengerCount=" + passengerCount +
                ", tpepPickupDatetime='" + tpepPickupDatetime + '\'' +
                ", pickupLatitude='" + pickupLatitude + '\'' +
                ", pickupLongitude='" + pickupLongitude + '\'' +
                ", dropoffLatitude='" + dropoffLatitude + '\'' +
                ", dropoffLongitude='" + dropoffLongitude + '\'' +
                '}';
    }
}
