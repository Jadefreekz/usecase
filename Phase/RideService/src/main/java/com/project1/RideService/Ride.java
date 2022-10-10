package com.project1.RideService;

public class Ride {
    public long rideId;
    public boolean isOngoing;
    public long cabID;
    public long sourceLoc;
    public long destinationLoc;

    public Ride(long rideId,long cabID, long sourceLoc,long destinationLoc, boolean isOngoing){
        this.rideId = rideId;
        this.cabID = cabID;
        this.sourceLoc = sourceLoc;
        this.isOngoing = isOngoing;
        this.destinationLoc = destinationLoc;
    }


    public long getRideId() {
        return rideId;
    }

    public boolean isRideOngoing()
    {
        return  isOngoing;
    }

    public void setIsOngoing(boolean ongoing) {
        isOngoing = ongoing;
    }

    public void setRideId(long rideId) {
        this.rideId = rideId;
    }

    public long getSourceLoc() {
        return sourceLoc;
    }

    public void setSourceLoc(long sourceLoc) {
        this.sourceLoc = sourceLoc;
    }

    public long getDestinationLoc() {
        return destinationLoc;
    }

    public void setDestinationLoc(long destinationLoc) {
        this.destinationLoc = destinationLoc;
    }

    public long getCabID() {
        return cabID;
    }

    public void setCabID(long cabID) {
        this.cabID = cabID;
    }
}
