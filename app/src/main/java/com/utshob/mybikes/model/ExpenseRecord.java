package com.utshob.mybikes.model;

import com.google.firebase.database.Exclude;

public class ExpenseRecord {
    private String id;
    private String bikeId = "";
    private long timestamp;
    private String category = "Other";
    private double amount;
    private String note = "";

    public ExpenseRecord() {}

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }
    public String getBikeId() { return bikeId; }
    public void setBikeId(String bikeId) { this.bikeId = bikeId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
