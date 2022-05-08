
package pods.cabs;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Random;

public class FulfillRide extends AbstractBehavior<FulfillRide.Command>{

    //interface for defining messages
    interface Command{}

    public static final class RideEnded implements FulfillRide.Command{
        String cabId;
        int rideId;
        RideEnded(String cabId, int rideId)
        {
            this.cabId = cabId;
            this.rideId = rideId;
        }
    }
    public static final class RequestRideResponse implements  FulfillRide.Command{
        boolean result;
        String cabId;
        int currPos;
        public RequestRideResponse(boolean result,String cabId, int currPos){
            this.result = result;
            this.cabId = cabId;
            this.currPos = currPos;
        }
    }

    public static final class RideStartedResponse implements  FulfillRide.Command{
        boolean result;
        public RideStartedResponse(boolean result){
            this.result = result;
        }
    }

    public static final class RideCancelledResponse implements  FulfillRide.Command{
        boolean result;
        public RideCancelledResponse(boolean result){
            this.result = result;
        }
    }

    public static final class ResponseBalance implements FulfillRide.Command{
        public final Wallet.ResponseBalance wr;
        public ResponseBalance(Wallet.ResponseBalance wr){
            this.wr = wr;
        }
    }

    public static final class RequestRide implements  FulfillRide.Command{
        public int sourceLoc;
        public int destinationLoc;
        public String CustId;

        public RequestRide(String CustId,
                           int sourceLoc,
                           int destinationLoc){
            this.CustId = CustId;
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
        }
    }

    private HashMap<String, RideService.Pair> internalCache;
    private  String cabId;
    private  int rideId = 0;
    private  int sourceLoc;
    private  int destinationLoc;
    private final String CustId;
    private  int fare;
    private final int RideServiceNo;
     int attempt = 0;
    int currPos;
    ActorRef<RideService.RideResponse> rr;

    public static Behavior<FulfillRide.Command> create(String custId,
                                                       int sourceLoc,
                                                       int destinationLoc,
                                                       HashMap<String, RideService.Pair> internalCache,
                                                       int rideServiceNo,
                                                       ActorRef<RideService.RideResponse> rr) {
        return Behaviors.setup(context -> new FulfillRide(context,
                                                            custId,
                                                            sourceLoc,
                                                            destinationLoc,
                                                            internalCache,
                                                            rideServiceNo,
                                                                rr));
    }

    //Constructor
    private FulfillRide(ActorContext<Command> context,
                        String CustId,
                        int sourceLoc,
                        int destinationLoc,
                        HashMap<String, RideService.Pair> internalCache,
                        int rideServiceNo,
    ActorRef<RideService.RideResponse> rr)
    {
        super(context);
        cabId = "";
        this.CustId = CustId;
        this.sourceLoc = sourceLoc;
        this.destinationLoc = destinationLoc;
        this.internalCache = internalCache;
        RideServiceNo = rideServiceNo;
        this.rr = rr;
        context.getLog().info("FulfillRide actor {}-{}-{}-{}-{} started",
                getContext().getSelf().path().name(), CustId,cabId,sourceLoc,destinationLoc);
    }

    @Override
    public Receive<FulfillRide.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(FulfillRide.RequestRide.class, this::onRequestRide)
                .onMessage(FulfillRide.RequestRideResponse.class, this::onRequestRideResponse)
                .onMessage(FulfillRide.ResponseBalance.class, this::onResponseBalance)
                .onMessage(FulfillRide.RideStartedResponse.class, this::onRideStartedResponse)
                .onMessage(FulfillRide.RideCancelledResponse.class, this::onRideCancelledResponse)
                .onMessage(FulfillRide.RideEnded.class, this::onRideEnded)
                .build();
    }

    String closestCab = null;
    String secondClosest = null;
    String thirdClosest = null;
    private Behavior<FulfillRide.Command>onRequestRide(FulfillRide.RequestRide r) {
        System.out.println("INTERNAL CACHE: " );
        for (String a : internalCache.keySet())
        {
            System.out.println("Cab " + a + " State: "  + internalCache.get(a).state);
        }
        int src = r.sourceLoc;

        if(attempt==0) {
            attempt++;
            System.out.println("Attempt no " + attempt + " by Customer " + CustId+
                    " inside" + getContext().getSelf().path().name());
            int min = Integer.MAX_VALUE;
            for (String name : internalCache.keySet()) {
                if ((Math.abs(internalCache.get(name).lastKnownLoc - src) < min) &&
                        (internalCache.get(name).state.equals("available"))) {
                    min = Math.abs(internalCache.get(name).lastKnownLoc - src);
                    closestCab = name;
                }
            }
            if(closestCab == null)
            {
                System.out.println("No cab available in attempt: " + attempt);
                ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
                rs.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
                rr.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
            }
            else
            {
                cabId = closestCab;
                ActorRef<Cab.Command> cabRef = Globals.cabs.get(closestCab);
                cabRef.tell(new Cab.RequestRide(getContext().getSelf()));
                System.out.println("First closest cab Found and request sent.");
            }

        }
        else if(attempt ==1){
            attempt++;
            System.out.println("Attempt no " + attempt + " by Customer " + CustId +
                    " inside" + getContext().getSelf().path().name());
            int min2 = Integer.MAX_VALUE;
            for(String name : internalCache.keySet()){
                if(!name.equals(closestCab) &&
                        (Math.abs(internalCache.get(name).lastKnownLoc - src) < min2) &&
                        (internalCache.get(name).state.equals("available"))){
                    min2 = Math.abs(internalCache.get(name).lastKnownLoc - src);
                    secondClosest = name;
                }
            }
            if(secondClosest == null)
            {
                System.out.println("No cab available in attempt: " + attempt);
                ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
                rs.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
                rr.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
            }
            else {
                System.out.println("Found cab in 2nd attempt");
                cabId = secondClosest;
                ActorRef<Cab.Command> cabRef = Globals.cabs.get(secondClosest);
                cabRef.tell(new Cab.RequestRide(getContext().getSelf()));
            }

        }
        else if(attempt==2){
            attempt++;
            System.out.println("Attempt no " + attempt + " by Customer " + CustId +
                    " inside" + getContext().getSelf().path().name());
            attempt++;
            int min3 = Integer.MAX_VALUE;
            for(String name : internalCache.keySet()){
                if(!name.equals(closestCab) &&
                        !name.equals(secondClosest) &&
                        (Math.abs(internalCache.get(name).lastKnownLoc - src) < min3) &&
                        (internalCache.get(name).state.equals("available"))){
                    min3 = Math.abs(internalCache.get(name).lastKnownLoc - src);
                    thirdClosest = name;
                }
            }
            if(thirdClosest  == null)
            {
                System.out.println("No cab available in attempt: " + attempt);
                ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
                rs.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
                rr.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
            }
            else {
                cabId = thirdClosest;
                ActorRef<Cab.Command> cabRef = Globals.cabs.get(thirdClosest);
                cabRef.tell(new Cab.RequestRide(getContext().getSelf()));
            }
        }
        else{
            ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
            rs.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
            rr.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
        }
        return this;
    }

    private Behavior<FulfillRide.Command> onRequestRideResponse(FulfillRide.RequestRideResponse r) {
        if(r.result==true){
            ActorRef<Wallet.Command> walletRef = Globals.wallets.get(CustId);
            //  ActorRef<FulfillRide.Command> fr = getContext().getSelf();
            cabId = r.cabId;
            ActorRef<Wallet.ResponseBalance> responseBalanceAdapter = getContext().messageAdapter(Wallet.ResponseBalance.class,FulfillRide.ResponseBalance::new);
            // ActorRef<Device.RespondTemperature> respondTemperatureAdapter = context.messageAdapter(Device.RespondTemperature.class, WrappedRespondTemperature::new);

            currPos = r.currPos;
            walletRef.tell(new Wallet.DeductBalance(1L,Math.abs(sourceLoc-destinationLoc)*10 + Math.abs(sourceLoc-currPos)*10,responseBalanceAdapter));

        }
        else{
            getContext().getSelf().tell(new RequestRide(CustId, sourceLoc, destinationLoc));
        }
        return this;
    }

    private Behavior<FulfillRide.Command> onResponseBalance(FulfillRide.ResponseBalance r){
        ActorRef<Cab.Command> cabRef = Globals.cabs.get(cabId);
        fare = Math.abs(sourceLoc-destinationLoc)*10 + Math.abs(sourceLoc-currPos)*10;
        if(r.wr.bal!=-1){
            cabRef.tell(new Cab.RideStarted(destinationLoc,getContext().getSelf()));
        }
        else{

            cabRef.tell(new Cab.RideCancelled(getContext().getSelf()));
            ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
            rs.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
            rr.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
        }
        return this;
    }

    private Behavior<FulfillRide.Command> onRideStartedResponse(FulfillRide.RideStartedResponse r){
        //Generate Ride Id
        if(r.result==true){

            /*RideService.Pair p = new RideService.Pair(internalCache.get(cabId).lastKnownLoc, "giving-ride");
            internalCache.replace(cabId, p);
            System.out.println("State for Cab " + cabId + " " + internalCache.get(cabId).state);*/
            rideId = Globals.rideId.getAndIncrement();
            ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
            rs.tell(new RideService.RideResponse(rideId,cabId,fare,getContext().getSelf()));
            rr.tell(new RideService.RideResponse(rideId,cabId,fare,getContext().getSelf()));
        }
        else{
            ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
            rs.tell(new RideService.RideResponse(-1,null,-1,getContext().getSelf()));
            rr.tell(new RideService.RideResponse(-1,null,-1,getContext().getSelf()));
            return Behaviors.stopped();
        }

        return this;
    }

    private Behavior<FulfillRide.Command> onRideCancelledResponse(FulfillRide.RideCancelledResponse r){

        ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
        rs.tell(new RideService.RideResponse(-1,cabId,-1,getContext().getSelf()));
        return Behaviors.stopped();

    }

    private Behavior<FulfillRide.Command> onRideEnded(FulfillRide.RideEnded r){
        ActorRef<RideService.Command> rs = Globals.rideService[RideServiceNo];
        rs.tell(new RideService.RideEnded(r.cabId,  rideId, destinationLoc));
        return Behaviors.stopped();
    }

}
