package pods.cabs;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Random;

class TempCabTuple{
    public static String cabId;
    public static String state;
    public static int reqNo;
    public static int numRides;
    public static int currPos;
    public static int source = -1;
    public static int dest  = -1;

}

public class RideService extends AbstractBehavior<RideService.Command>
{
    //RideDetails class equivalent to the ride database.
    public static class RideDetails
    {
        private boolean isOngoing;
        private int rideId;
        private String cabId;
        private int source;
        private int dest;
        private int fare;

        public RideDetails(int rideId, String cabId, int source, int dest, int fare, boolean isOngoing)
        {
            this.isOngoing = isOngoing;
            this.rideId = rideId;
            this.cabId = cabId;
            this.source = source;
            this.dest = dest;
            this.fare = fare;
        }

    }


    //internal cache table for storing cabs and their last-known location
    //is the value in key-value hashmap where key is the cabId String
    public static class Pair{
        int lastKnownLoc;
        String state;
        public Pair(int lastKnownLoc,String state){
            this.lastKnownLoc = lastKnownLoc;
            this.state = state;
        }
    }

    //Inerface as placeholder for RideService Messages
    interface Command{}

    public static final class RideEnded implements  RideService.Command
    {
        String cabId;
        int rideId;
        int destination;
        public RideEnded(String cabId, int rideId, int destination) {
            this.cabId = cabId;
            this.rideId = rideId;
            this.destination = destination;
        }
    }
    //Message type for  signing in
    public static final class CabSignsIn implements RideService.Command {
        String cabId;
        int initialPos;
        public CabSignsIn(String cabId,int initialPos) {
            this.cabId = cabId;
            this.initialPos = initialPos;
        }
    }
    //Converted message type(for use by adapter)
    public static class WrappedCabDetails implements RideService.Command
    {
        final Cab.RespondCabDetails response;

        WrappedCabDetails(Cab.RespondCabDetails response)
        {
            this.response = response;
        }
    }

    //Message type for signing out
    public static final class CabSignsOut implements RideService.Command {
        String cabId;
        public CabSignsOut(String cabId) {
            this.cabId = cabId;
        }
    }

    ////Message type for requesting a ride
    public static final class RequestRide implements RideService.Command {
        String custId;
        int sourceLoc;
        int destinationLoc;
        ActorRef<RideService.RideResponse> replyTo;
        public RequestRide(String custId, int sourceLoc, int destinationLoc, ActorRef<RideService.RideResponse> replyTo){
            this.custId = custId;
            this.sourceLoc = sourceLoc;
            this.destinationLoc = destinationLoc;
            this.replyTo = replyTo;
        }
    }

    //Message type to collect ride reponse
    public static final class RideResponse implements RideService.Command {
        int rideId;
        String cabId;
        int fare;
        ActorRef<FulfillRide.Command> fRide;
        public RideResponse(int rideId, String cabId, int fare, ActorRef<FulfillRide.Command> fRide){
            this.rideId = rideId;
            this.cabId = cabId;
            this.fare = fare;
            this.fRide = fRide;
        }
    }

    public static final class SetAvailable implements RideService.Command
    {
        String cabId;
        int destination;
        int rideId;

        public SetAvailable(String cabId, int destination, int rideId) {
            this.cabId = cabId;
            this.destination = destination;
            this.rideId = rideId;
        }
    }

    public static final class UpdateSignInOrOut implements RideService.Command
    {
        String cabId;
        boolean signIn;
        int initPosIfSignIn;
        UpdateSignInOrOut(String cabId, boolean signIn, int initPosIfSignIn)
        {
            this.cabId = cabId;
            this.signIn = signIn;
            this.initPosIfSignIn = -1;
        }
    }


    //Message type to indicate update
    public static final class UpdateTable implements RideService.Command
    {
        RideDetails rd;
        RideService.RideResponse r;

        UpdateTable(RideDetails rd, RideService.RideResponse r)
        {
            this.rd = rd;
            this.r = r;
        }
    }


    //internal cache table
    public static HashMap<String,Pair> internalCache;
    //table for ride details, contains everything.
    public static HashMap<Integer, RideDetails> rideDetails;
    //To indicate which rideService actor out of 10 we are referring to
    public int RideServiceNo;

    //Create a RideService actor
    public static Behavior<Command> create(int RideServiceNo) {
        return Behaviors.setup(context -> new RideService(context, RideServiceNo));
    }

    //Constructor
    private RideService(ActorContext<Command> context,int RideServiceNo)  {
        super(context);
        internalCache = new HashMap<>();
        rideDetails = new HashMap<>();
        this.RideServiceNo = RideServiceNo;

        //initialising cache table[internalCache]
        int count = 0;
        for(String s : Globals.cabs.keySet())
        {


            ActorRef<Cab.Command> cabActorTemp = Globals.cabs.get(s);
            ActorRef<Cab.RespondCabDetails> respondRef =
                    context.messageAdapter(Cab.RespondCabDetails.class, WrappedCabDetails::new);
            cabActorTemp.tell(new Cab.GetCabDetails(count, respondRef));

            //System.out.println(s+ ", Pos: " + TempCabTuple.currPos + ", Pos: "+  TempCabTuple.state);
            //internalCache.put(s, new Pair(TempCabTuple.currPos, TempCabTuple.state));

            count++;
        }

    }


    @Override
    public Receive<RideService.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(RideService.CabSignsIn.class, this::onCabSignsIn)
                .onMessage(RideService.CabSignsOut.class, this::onCabSignOut)
                .onMessage(RideService.RequestRide.class, this::onRequestRide)
                .onMessage(RideService.WrappedCabDetails.class, this::onReceiveCabDetails)
                .onMessage(RideService.RideResponse.class, this::onRideResponse)
                .onMessage(RideService.UpdateTable.class, this::onUpdateTable)
                .onMessage(RideService.UpdateSignInOrOut.class, this::onUpdateSignInOrOut)
                .onMessage(RideService.RideEnded.class, this::onRideEnded)
                .onMessage(RideService.SetAvailable.class, this::onSetAvailable)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<RideService.Command> onRideEnded(RideEnded r)
    {
        System.out.println("In RideEnded for :" + r.rideId + ", CAB: " + r.cabId);
        Pair p = internalCache.get(r.cabId);
        p.state = "available";
        p.lastKnownLoc = r.destination;
        internalCache.replace(r.cabId, p);
        for(int i = 0; i<10; i++)
        {
            if(i != RideServiceNo)
            {
                Globals.rideService[i].tell(new SetAvailable(r.cabId, r.destination, r.rideId));
            }
        }

        return this;
    }

    private Behavior<RideService.Command> onSetAvailable(SetAvailable s)
    {
        internalCache.replace(s.cabId, new Pair(s.destination, "available"));
        RideDetails rd = rideDetails.get(s.rideId);
        int source = rd.source;
        int dest = rd.dest;
        int fare = rd.fare;
        rideDetails.replace(s.rideId, new RideDetails(s.rideId, s.cabId, source, dest, fare, false));
        return this;
    }

    private Behavior<RideService.Command> onUpdateSignInOrOut(UpdateSignInOrOut up)
    {
        if(up.signIn == true)
            internalCache.replace(up.cabId, new Pair(up.initPosIfSignIn, "available"));
        else
        {
            int lkl = internalCache.get(up.cabId).lastKnownLoc;
            internalCache.replace(up.cabId, new Pair(lkl, "signed-out"));
        }
        return this;
    }
    private Behavior<RideService.Command> onCabSignsIn(CabSignsIn cSOut)
    {
        System.out.println("in cabSignsIn");
        String cabId = cSOut.cabId;
        int pos = cSOut.initialPos;
        Pair p = internalCache.get(cabId);
        p.lastKnownLoc = pos;
        p.state = "available";
        internalCache.replace(cabId, p);
        for(int i = 0; i<10; i++)
        {
            if(i == RideServiceNo)
            continue;
                else {
            Globals.rideService[i].tell(new UpdateSignInOrOut(cabId, true, pos));
            }
        }
        return this;
    }

    private Behavior<RideService.Command> onCabSignOut(CabSignsOut cSIn)
    {
        System.out.println("in onCabSignOut");
        String cabId = cSIn.cabId;
        Pair p = internalCache.get(cabId);
        p.state = "signed-out";
        internalCache.replace(cabId, p);
        for(int i = 0; i<10; i++)
        {
            if(i == RideServiceNo)
                continue;
            else {
                Globals.rideService[i].tell(new UpdateSignInOrOut(cabId, false, -1));
            }
        }
        return this;
    }

    private Behavior<RideService.Command> onRequestRide(RequestRide request)
    {
        System.out.println("inside onRequestRide");
        TempCabTuple.source = request.sourceLoc;
        TempCabTuple.dest = request.destinationLoc;
        Random random = new Random();
        ActorRef<FulfillRide.Command> fr = getContext().spawn(FulfillRide.create(request.custId,
                                                                                request.sourceLoc,
                                                                                request.destinationLoc,
                                                                                internalCache,
                                                                                RideServiceNo,
                                                                                request.replyTo),
                                                                "FullfillRequestRide" + Integer.toString(random.nextInt()));


        fr.tell(new FulfillRide.RequestRide(request.custId, request.sourceLoc, request.destinationLoc));
        return this;
    }

    private Behavior<RideService.Command> onReceiveCabDetails(WrappedCabDetails wcd)
    {
        //System.out.println("in onReceiveCabDetails");
        var  a = wcd.response;

        TempCabTuple.cabId = a.sendCabId;
        TempCabTuple.state = a.sendState;
        TempCabTuple.reqNo = a.sendReqNo;
        TempCabTuple.numRides = a.sendNumRides;
        TempCabTuple.currPos = a.sendCurrPos;

        /*System.out.println(TempCabTuple.cabId + " " +
                TempCabTuple.state + " " +
                TempCabTuple.reqNo + " " +
                TempCabTuple.numRides + " " +
                TempCabTuple.currPos);*/
        internalCache.put(TempCabTuple.cabId, new Pair(TempCabTuple.currPos, TempCabTuple.state));
        //System.out.println(internalCache);
        return this;
    }

    private Behavior<RideService.Command> onRideResponse(RideResponse r)
    {
        //Update RideDetails class
        RideDetails rd = new RideDetails(r.rideId, r.cabId, TempCabTuple.source, TempCabTuple.dest, r.fare, true);
        if(r.rideId != -1 && r.fare >= 0){
            rideDetails.put(r.rideId, rd);
            for (int i = 0; i < 10; i++) {

                    Globals.rideService[i].tell(new UpdateTable(rd, r));

            }
        }

        return this;
    }

    private Behavior<RideService.Command> onUpdateTable(UpdateTable u)
    {
        internalCache.replace(u.r.cabId, new Pair(u.rd.source, "giving-ride"));
        rideDetails.put(u.rd.rideId, u.rd);

        return this;
    }

    private Behavior<Command> onPostStop()
    {
        getContext().getLog().info("RideService actor {} stopped", this.RideServiceNo);

        return Behaviors.stopped();
    }

}

