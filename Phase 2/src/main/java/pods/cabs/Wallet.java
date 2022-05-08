package pods.cabs;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Wallet extends AbstractBehavior<Wallet.Command> {



    //Responsible for creating the actor
    interface Command{}
    public static final class GetBalance implements Command {
        final long requestId;
        public final ActorRef<ResponseBalance> replyTo;
        public GetBalance(long requestId,ActorRef<ResponseBalance> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }
    public static final class DeductBalance implements Command {
        public final ActorRef<ResponseBalance> replyTo;
        public final int toDeduct;
        public final long requestId;
        public DeductBalance(long requestId,int toDeduct, ActorRef<ResponseBalance> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
            this.toDeduct = toDeduct;
        }
    }
    public static final class AddBalance implements Command {
        public final int toAdd;
        public AddBalance(int toAdd){
            this.toAdd = toAdd;
        }
    }
    public static final class Reset implements Command {
        public final long requestId;
        public final ActorRef<ResponseBalance> replyTo;
        public Reset(long requestId,ActorRef<ResponseBalance> replyTo){
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }
    public static final class ResponseBalance implements Command{
        public final long requestId;
        public final int bal;
        public ResponseBalance(long requestId,int bal){
            this.requestId = requestId;
            this.bal = bal;
        }
    }
    public static Behavior<Command> create(String CustId) {
        return Behaviors.setup(context -> new Wallet(context, CustId));
    }

    //customer ID and balances
    private static int currBal;
    private final String CustId;

    private Wallet(ActorContext<Command> context, String CustId) {
        super(context);
        this.CustId = CustId;
        Wallet.currBal = 10000;
        context.getLog().info("Wallet actor {}-{} started", CustId);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(GetBalance.class, this::onGetBalance)
                .onMessage(DeductBalance.class, this::onDeductBalance)
                .onMessage(AddBalance.class, this::onAddBalance)
                .onMessage(Reset.class,this::onReset)
                .onSignal(PostStop.class, signal -> onPostStop())
                .build();
    }

    private Behavior<Command> onGetBalance(GetBalance r) {
        r.replyTo.tell(new ResponseBalance(r.requestId, currBal));
        return this;
    }

    private Behavior<Command> onDeductBalance(DeductBalance r) {
        int ans;
        if(currBal >= r.toDeduct&&r.toDeduct>=0){
           currBal-= r.toDeduct;
           ans = currBal;
        }
        else{
           ans = -1;
        }
        r.replyTo.tell(new ResponseBalance(r.requestId,ans));
        return this;
    }

    private Behavior<Command> onAddBalance(AddBalance r){
        if(r.toAdd>=0)
          currBal+=r.toAdd;

        return this;
    }
    private Behavior<Command> onReset(Reset r){
        currBal = 10000;
        r.replyTo.tell(new ResponseBalance(r.requestId,currBal));
        return this;
    }
    private Behavior<Command> onPostStop() {
        getContext().getLog().info("Wallet actor {}-{} stopped", CustId);
        return Behaviors.stopped();
    }


}
