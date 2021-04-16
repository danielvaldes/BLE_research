package com.example.and_medicalprototyping;

public class WeightEntry {
    private Double weight;
    private String timeStamp;
    private Boolean metric; //Kg\lb
    private int rssi;

    public WeightEntry()
    {
        weight=0.0;
        timeStamp="0/0/0";
        metric=false; //==lb
        rssi=0;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
    public void setTimeStamp(String st)    {
        this.timeStamp = st;
    }
    public void setMetric(Boolean mt)    {
        this.metric = mt;
    }
    public void setRssi(int rs)
    {
        this.rssi = rs;
    }
    public Boolean getMetric() {
        return this.metric;
    }
    public String getTimeStamp() {
        return this.timeStamp;
    }
    public Double getWeight() {
        return this.weight;
    }
    public Integer getRssi()
    {
        return this.rssi;
    }
}
