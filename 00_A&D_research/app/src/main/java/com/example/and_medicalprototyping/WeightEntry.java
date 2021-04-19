package com.example.and_medicalprototyping;

public class WeightEntry {
    private Double weight;
    private String metric;
    private int rssi;

    public WeightEntry()
    {
        weight=0.0;
        metric=""; //lb or kg
        rssi=0;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
    public Double getWeight() {
        return this.weight;
    }

    public void setMetric(String mt)    {
        this.metric = mt;
    }
    public String getMetric() {
        return this.metric;
    }

    public void setRssi(int rs)
    {
        this.rssi = rs;
    }
    public Integer getRssi()
    {
        return this.rssi;
    }

}
