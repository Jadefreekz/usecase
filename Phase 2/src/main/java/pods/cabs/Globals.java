package pods.cabs;

import akka.actor.typed.ActorRef;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Globals{
    public static final Map<String, ActorRef<Wallet.Command>> wallets = new HashMap<>();
    public static final Map<String, ActorRef<Cab.Command>> cabs = new HashMap<>();
    public static final ActorRef<RideService.Command> rideService[] = new ActorRef[10];
    public static final AtomicInteger rideId = new AtomicInteger(401);


}
