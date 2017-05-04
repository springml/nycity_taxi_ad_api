package com.springml.nyc.taxi.ad.api.model;

import java.util.List;

public class Predictions {

    private List<Prediction> predictions;

    public List<Prediction> getPredictions() {
        return predictions;
    }

    public void setPredictions(List<Prediction> predictions) {
        this.predictions = predictions;
    }

    @Override
    public String toString() {
        return "Predictions{" +
                "predictions=" + predictions +
                '}';
    }
}