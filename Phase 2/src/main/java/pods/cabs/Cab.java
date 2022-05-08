package pods.cabs;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import java.util.Random;

public class Cab extends AbstractBehavior<Cab.Command> {

    private final String cabId;
    private  String state;
    private  int reqNo;
    private  int numRides;
    private  int currPos;
    private ActorRef<FulfillRide.Command> fr;

    public Cab(ActorContext<Command> context,
               String cabId,
               String state,
               int reqNo,
               int numRides,
               int currPos)
    {
        super(context);
        this.cabId = cabId;
        this.state = state;
        this.reqNo = reqNo;
        this.numRides = numRides;
        this.currPos = currPos;

    }
    //Interface as placeholder for Cab Messages
    interface Command{}
    
    public static final class GetCabDetails implements Cab.Command
    {
        private long requestId;
        ActorRef<Cab.RespondCabDetails> replyTo;

        public GetCabDetails(int requestId, ActorRef<RespondCabDetails> replyTo)
        {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static final class RespondCabDetails implements Cab.Command{
        public long requestId;
        public String sendCabId;
        public String sendState;
        public int sendReqNo;
        public  int sendNumRides;
        public int sendCurrPos;

        public RespondCabDetails(int requestId, String cabId, String state, int reqNo, int numRides, int currPos)
        {
            this.requestId = requestId;
            this.sendCabId = cabId;
            this.sendState = state;
            this.sendReqNo = reqNo;
            this.sendNumRides = numRides;
            this.sendCurrPos = currPos;
        }

    }

    public static final class RideEnded implements Cab.Command {
        int rideId;
        public RideEnded(int rideId) {
            this.rideId = rideId;
        }
    }

    public static final class SignIn implements Cab.Command{
        private int initialPos;
        public SignIn(int initialPos){
            this.initialPos = initialPos;
        }
    }

    public static final class SignOut implements Cab.Command{
        public SignOut(){

        }

    }

    public static final class NumRidesResponse implements Cab.Command{
        public int numRides;
        public NumRidesResponse(int numRides){
            this.numRides = numRides;
        }
    }
    public static final class NumRides implements Cab.Command{
        private ActorRef<Cab.NumRidesResponse> replyTo;
        public NumRides(ActorRef<Cab.NumRidesResponse> replyTo){
            this.replyTo = replyTo;
        }
    }

    public static final class Reset implements Cab.Command{
        private ActorRef<Cab.NumRidesResponse> replyTo;
        public Reset(ActorRef<Cab.NumRidesResponse> replyTo){
            this.replyTo = replyTo;
        }
    }

    public static final class RequestRide implements Cab.Command{
        private ActorRef<FulfillRide.Command> replyTo;
        public RequestRide(ActorRef<FulfillRide.Command> replyTo){
            this.replyTo = replyTo;
        }
    }

    public static final class RideStarted implements Cab.Command{
        int dest;
        private ActorRef<FulfillRide.Command> replyTo;
        public RideStarted(int dest, ActorRef<FulfillRide.Command> replyTo){
            this.dest = dest;
            this.replyTo = replyTo;
        }
    }
    public static final class RideCancelled implements Cab.Command{
        private ActorRef<FulfillRide.Command> replyTo;
        public RideCancelled(ActorRef<FulfillRide.Command> replyTo){
            this.replyTo = replyTo;
        }
    }

    public static Behavior<Cab.Command> create(String cabId, String state, int reqNo, int numRides, int currPos) {
        return Behaviors.setup(context -> new Cab(context, cabId, state, reqNo, numRides, currPos));
    }

    @Override
    public Receive<Cab.Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(Cab.RideEnded.class, this::onRideEnded)
                .onMessage(Cab.SignIn.class, this::onSignIn)
                .onMessage(Cab.SignOut.class, this::onSignOut)
                .onMessage(Cab.NumRides.class,this::onNumRides)
                .onMessage(Cab.Reset.class,this::onReset)
                .onMessage(Cab.RequestRide.class,this::onRequestRide)
                .onMessage(Cab.RideStarted.class,this::onRideStarted)
                .onMessage(Cab.RideCancelled.class,this::onRideCancelled)
                .onMessage(Cab.GetCabDetails.class, this::onGetCabDetails)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }


    private Behavior<Cab.Command> onRideEnded(RideEnded r) {
        if (state.equals("giving-ride")) {
            fr.tell(new FulfillRide.RideEnded(cabId, -1));
            state = "available";
        }
        return this;
    }

    private Behavior<Cab.Command> onGetCabDetails(GetCabDetails r) {
        r.replyTo.tell(new RespondCabDetails(
                0,
                cabId,
                state,
                reqNo,
                numRides,
                currPos
        ));
        return this;
    }

    private Behavior<Cab.Command> onSignIn(SignIn r) {
        Random rand = new Random();
        int idx = rand.nextInt(10);
        ActorRef<RideService.Command> rs = Globals.rideService[idx];
        if(state.equals("signed-out"))
        {
            rs.tell(new RideService.CabSignsIn(cabId, r.initialPos));
            state = "available";
            currPos = r.initialPos;
        }
        else
            System.out.println("Already signed-in. Can't sign-in again");
        return this;
    }
    private Behavior<Cab.Command> onSignOut(SignOut r) {
        Random rand = new Random();
        int idx = rand.nextInt(10);
        if(state.equals("available")){
            ActorRef<RideService.Command> rs = Globals.rideService[idx];
            rs.tell(new RideService.CabSignsOut(cabId));
            state = "signed-out";
        }
        else
            System.out.println("Can't sign out. Cab is either giving ride/committed/already signed-out");
        return this;
    }
    private Behavior<Cab.Command> onNumRides(NumRides r) {
        r.replyTo.tell(new NumRidesResponse(numRides));
        return this;
    }
    private Behavior<Cab.Command> onReset(Reset r) {

        if(!state.equals("signed-out"))
        {
            int tempNumRides = numRides;
            if (state.equals("giving-ride")) {
                fr.tell(new FulfillRide.RideEnded(cabId, -1));
            }
            getContext().getSelf().tell(new SignOut());
            numRides = 0;
            reqNo = 0;
            state = "signed-out";
            currPos = -1;

            r.replyTo.tell(new NumRidesResponse(tempNumRides));
        }
        return this;
    }
    private Behavior<Cab.Command> onRequestRide(RequestRide r) {
        reqNo=reqNo+1;
        System.out.println("RequestNo:::::: " + reqNo + " on Cab " + cabId);
        if(state.equals("available") && reqNo%2==1){
            state = "committed";
            r.replyTo.tell(new FulfillRide.RequestRideResponse(true, cabId, currPos));
        }
        else{
            System.out.println("State: " + state);
            r.replyTo.tell(new FulfillRide.RequestRideResponse(false, cabId, currPos));
        }
        return this;
    }

    private Behavior<Cab.Command>onRideStarted(RideStarted r) {
        if(state.equals("committed")){
            numRides=numRides+1;
            state = "giving-ride";
            currPos = r.dest;
            fr = r.replyTo;
            r.replyTo.tell(new FulfillRide.RideStartedResponse(true));
        }
        else{
            r.replyTo.tell(new FulfillRide.RideStartedResponse(false));
        }
        return this;
    }

    private Behavior<Cab.Command>onRideCancelled(RideCancelled r) {
        if(state.equals("committed")) {
            state = "available";
            r.replyTo.tell(new FulfillRide.RideCancelledResponse(true));
        }
        else{
            r.replyTo.tell(new FulfillRide.RideCancelledResponse(false));
        }
        return this;
    }

    private Behavior<Cab.Command> onPostStop() {
        getContext().getLog().info("Cab actor {}-{} stopped", cabId);
        return Behaviors.stopped();
    }
}