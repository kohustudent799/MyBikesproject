package com.utshob.mybikes.model;

import com.google.firebase.database.Exclude;

public class Bike {
    private String id;
    private String name = "";
    private String brand = "";
    private String model = "";
    private String regNo = "";
    private int engineCc;
    private long purchaseDate;
    private String fuelType = "Petrol";
    private double odometer;
    private String photoUrl = "";

    public Bike() {}

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public int getEngineCc() { return engineCc; }
    public void setEngineCc(int engineCc) { this.engineCc = engineCc; }
    public long getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(long purchaseDate) { this.purchaseDate = purchaseDate; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public double getOdometer() { return odometer; }
    public void setOdometer(double odometer) { this.odometer = odometer; }
    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
