package com.project1.Cab;
import java.util.ArrayList;

public class Cab {
    enum STATES
    {
        available, committed, giving_ride;
    }

    private long cabId;
    private long requestNo;
    private long custId;
    ArrayList<Long> rideHistory;
    private long rideId;
    private long initialPos;
    private long currPos;
    private long sourcePos;
    private long destPos;
    private String state;
    private int numRides;

    public Cab(long cabId,long rideId, long initialPos,long currPos, String state) {
        this.cabId = cabId;
        this.rideId = rideId;
        this.rideHistory = new ArrayList<Long>();
        this.initialPos = initialPos;
        this.currPos = currPos;
        this.state = "signed-out";
        this.requestNo = 0;
    }

    public long getCabId() {
        return cabId;
    }
    public void setCabId(long cabId) {
        this.cabId = cabId;
    }

    public long getSourcePos() {
        return sourcePos;
    }

    public void setSourcePos(long sourcePos) {
        this.sourcePos = sourcePos;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public long getRideId() {
        return rideId;
    }

    public void setRideId(long rideId) {
        this.rideId = rideId;
    }

    public long getRequestNo() {
        return requestNo;
    }

    public void incReqNo()
    {
        requestNo++;
    }

    public void setRequestNo(long requestNo) {
        this.requestNo = requestNo;
    }

    public long getInitialPos() {
        return initialPos;
    }

    public void setInitialPos(long initialPos) {
        this.initialPos = initialPos;
    }

    public long getCurrPos() {
        return currPos;
    }

    public void setCurrPos(long currPos) {
        this.currPos = currPos;
    }

    public long getDestPos() {
        return destPos;
    }

    public void setDestPos(long destPos) {
        this.destPos = destPos;
    }

    public long getCustId() {
        return custId;
    }

    public void setCustId(long custId) {
        this.custId = custId;
    }

    public int getNumRides() {
        return numRides;
    }

    public void incRideNo()
    {
        numRides++;
    }
    public void setRideZero(){
        this.numRides=0;
    }

}