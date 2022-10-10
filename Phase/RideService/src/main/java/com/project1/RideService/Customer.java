package com.project1.RideService;

public class Customer {
    private long custId;
    private long src;
    private long dest;
    private long wallet;

    public Customer(long custId, long src,long dest,long wallet){
        this.custId = custId;
        this.src = src;
        this.dest = dest;
        this.wallet = wallet;
    }

    public long getDest() {
        return dest;
    }

    public long getSrc() {
        return src;
    }

    public long getWallet() {
        return wallet;
    }
    public long getCustId() {
        return custId;
    }

    public void setWallet(long amount)
    {
        wallet = amount;
    }

    public void setSrc(long src){
        this.src = src;
    }
    public void setDest(long dest){
        this.dest = dest;
    }
}