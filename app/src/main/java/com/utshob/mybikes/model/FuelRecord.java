package com.utshob.mybikes.model;

import com.google.firebase.database.Exclude;

public class FuelRecord {
    private String id;
    private String bikeId = "";
    private long timestamp;
    private double odometer;
    private String fuelType = "Petrol";
    private double pricePerLitre;   // the price actually used for THIS record (never changes later)
    private double totalCost;
    private double litres;
    private boolean fullTank;

    // Calculated on the phone, never saved to the database.
    private double mileage = Double.NaN;
    private double costPerKm = Double.NaN;

    public FuelRecord() {}

    @Exclude public String getId() { return id; }
    @Exclude public void setId(String id) { this.id = id; }
    public String getBikeId() { return bikeId; }
    public void setBikeId(String bikeId) { this.bikeId = bikeId; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public double getOdometer() { return odometer; }
    public void setOdometer(double odometer) { this.odometer = odometer; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public double getPricePerLitre() { return pricePerLitre; }
    public void setPricePerLitre(double pricePerLitre) { this.pricePerLitre = pricePerLitre; }
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    public double getLitres() { return litres; }
    public void setLitres(double litres) { this.litres = litres; }
    public boolean isFullTank() { return fullTank; }
    public void setFullTank(boolean fullTank) { this.fullTank = fullTank; }

    @Exclude public double getMileage() { return mileage; }
    @Exclude public void setMileage(double mileage) { this.mileage = mileage; }
    @Exclude public double getCostPerKm() { return costPerKm; }
    @Exclude public void setCostPerKm(double costPerKm) { this.costPerKm = costPerKm; }
    @Exclude public boolean hasMileage() { return !Double.isNaN(mileage); }
}
