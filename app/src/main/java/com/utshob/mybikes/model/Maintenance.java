package com.utshob.mybikes.model;

/** One Maintenance object per bike: engine oil reminder + air filter reminder. */
public class Maintenance {
    // Engine oil
    private long oilLastDate;
    private double oilLastKm;
    private double oilIntervalKm;       // 0 = not set
    private boolean oilDone;
    // Air filter (-1 / 0 mean "not set" for the optional values)
    private long airInstallDate;
    private double airInstallKm = -1;
    private long airNextDate;
    private double airNextKm = -1;
    private boolean airDone;

    public Maintenance() {}

    public long getOilLastDate() { return oilLastDate; }
    public void setOilLastDate(long v) { oilLastDate = v; }
    public double getOilLastKm() { return oilLastKm; }
    public void setOilLastKm(double v) { oilLastKm = v; }
    public double getOilIntervalKm() { return oilIntervalKm; }
    public void setOilIntervalKm(double v) { oilIntervalKm = v; }
    public boolean isOilDone() { return oilDone; }
    public void setOilDone(boolean v) { oilDone = v; }
    public long getAirInstallDate() { return airInstallDate; }
    public void setAirInstallDate(long v) { airInstallDate = v; }
    public double getAirInstallKm() { return airInstallKm; }
    public void setAirInstallKm(double v) { airInstallKm = v; }
    public long getAirNextDate() { return airNextDate; }
    public void setAirNextDate(long v) { airNextDate = v; }
    public double getAirNextKm() { return airNextKm; }
    public void setAirNextKm(double v) { airNextKm = v; }
    public boolean isAirDone() { return airDone; }
    public void setAirDone(boolean v) { airDone = v; }
}
