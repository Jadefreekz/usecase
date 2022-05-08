package pods.cabs;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Scanner;

public class Main extends AbstractBehavior<Main.Started> {

    public interface Command extends Serializable {}

    public static class Started implements Command {
        boolean response;
        public Started(boolean response) {
            this.response = response;
        }
    }
    public static Behavior<Started> create(ActorRef<Started> ref) {
        return Behaviors.setup(context -> new Main(context,ref));
    }
    static ArrayList<String> cabs = new ArrayList<String>();
    static ArrayList<String> cust = new ArrayList<String>();
    ActorRef<Started> ref ;
    public Main(ActorContext<Started> context,ActorRef<Started> ref) throws FileNotFoundException {
        super(context);
        this.ref = ref;
        long i = 0;

        File myObj = new File("IDs.txt");
        Scanner myReader = new Scanner(myObj);
        int initialBalance=0;
        while (myReader.hasNextLine()) {
            String data = myReader.nextLine();
            if (data.contains("****")) {
                i++;
                continue;
            }
            if (i == 1) {

                cabs.add(data);
            }
            if(i == 2){

                cust.add(data);
            }
            if (i == 3) {
                initialBalance = Integer.parseInt(data);

            }
        }

        myReader.close();
        myReader.close();
    }
    @Override
    public Receive<Started> createReceive() {
        return newReceiveBuilder().onMessage(Started.class, this::onCalled).build();
    }

    public Behavior<Started> onCalled(Started command){

        for(int j=0;j<cust.size();j++){
            ActorRef<Wallet.Command> ans =  getContext().spawn(Wallet.create(cust.get(j)),"Customer"+j);
            Globals.wallets.put(cust.get(j),ans);
        }

        for(int j=0;j<cabs.size();j++){
            ActorRef<Cab.Command> ans =
                    getContext().spawn(Cab.create(
                            cabs.get(j),
                            "signed-out",
                            0,
                            0,
                            -1
                            ),
                            "Cab"+j);

            Globals.cabs.put(cabs.get(j), ans);
        }

        for(int j = 0;j<10;j++){
            Globals.rideService[j] = getContext().spawn(RideService.create(j), "RideService"+j);
        }
        getContext().getLog().info("Wallet map" + Globals.rideService);
        getContext().getLog().info("CAb map" + Globals.cabs);

        this.ref.tell(new Started(true));

        return this;// return Behaviors.setup(Main::new);
    }
}



