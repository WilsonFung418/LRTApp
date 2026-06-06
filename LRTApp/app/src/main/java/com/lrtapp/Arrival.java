package com.lrtapp;

public class Arrival {
    public String routeNo;
    public String destinationEn;
    public String destinationTc;
    public String arrivalEn;
    public String arrivalTc;
    public int trainLength;
    public int platformId;
    public boolean isPlatformHeader;

    public String getRouteNo() { return routeNo; }
    public String getDestinationTc() { return destinationTc; }
    public String getDestinationEn() { return destinationEn; }
    public String getArrivalDisplay() {
        if (arrivalTc != null && !arrivalTc.isEmpty()) return arrivalTc;
        if (arrivalEn != null && !arrivalEn.isEmpty()) return arrivalEn;
        return "-";
    }
    public String getTrainLengthDisplay() {
        return trainLength == 0 ? "-" : String.valueOf(trainLength);
    }
}
