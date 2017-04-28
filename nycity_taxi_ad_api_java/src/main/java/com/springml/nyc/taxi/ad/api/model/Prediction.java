package com.springml.nyc.taxi.ad.api.model;

import java.util.List;


public class Prediction {

    private List<Double> probabilities = null;
    private List<Double> logits = null;
    private Integer classes;

    public List<Double> getProbabilities() {
        return probabilities;
    }

    public void setProbabilities(List<Double> probabilities) {
        this.probabilities = probabilities;
    }

    public List<Double> getLogits() {
        return logits;
    }

    public void setLogits(List<Double> logits) {
        this.logits = logits;
    }

    public Integer getClasses() {
        return classes;
    }

    public void setClasses(Integer classes) {
        this.classes = classes;
    }

    @Override
    public String toString() {
        return "Prediction{" +
                "probabilities=" + probabilities +
                ", logits=" + logits +
                ", classes=" + classes +
                '}';
    }
}
