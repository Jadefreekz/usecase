package pods.cabs;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import akka.testkit.javadsl.TestKit;
import org.junit.ClassRule;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

//#definition
public class CabServiceTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testCase9() throws InterruptedException{
        //This Testcase do the stress test.
        // Also verifies the consistency of the balance in customer wallet
        // ..after a series of ride requests.
        int allotCount=0,denialCount=0;
        System.out.println("### Test Case 9 Started ###");
        TestProbe<Main.Started> testProbe = testKit.createTestProbe();
        ActorRef<Main.Started> MainAct = testKit.spawn(Main.create(testProbe.getRef()), "main9");
        MainAct.tell(new Main.Started(false));
        assertEquals(true, testProbe.receiveMessage().response);

        TestProbe<Cab.NumRidesResponse> Cprobe = testKit.createTestProbe();
        ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");


        //Cabs sign In
        cab101.tell(new Cab.SignIn(3));

        cab101.tell(new Cab.NumRides(Cprobe.ref()));
        Cab.NumRidesResponse Cresp = Cprobe.receiveMessage();
        assertTrue(Cresp.numRides == 0);

        System.out.println("### Cab 101 is  Signed In");

        //Request For a ride and should be allowed with one request itself as Cab.NumRides had response
        TestProbe<RideService.RideResponse> probe = testKit.createTestProbe();
        int step=3, noOfSteps=200;
        for(int i=0;i<noOfSteps;i++)
        {
            ActorRef<RideService.Command> rideService = Globals.rideService[i%10];

            rideService.tell(new RideService.RequestRide("201", 0, step, probe.ref()));
            RideService.RideResponse resp = probe.receiveMessage();

            if(resp.rideId !=-1) {
                System.out.println("$$$$ Ride request from Customer 201 is accepted - Ride ID : " + resp.rideId);
                cab101.tell(new Cab.RideEnded(resp.rideId));
                System.out.println("$$$$ Ride Has ended");
                allotCount++;
            }
            else {
                System.out.println("$$$$ Ride request from Customer 201 is Denied ! ");
                denialCount++;
            }
        }

        System.out.println("#### Total Rides : " + allotCount + " Denial of rides : " + denialCount);

        ActorRef<Wallet.Command> cust201 = Globals.wallets.get("201");
        TestProbe<Wallet.ResponseBalance> Wprobe = testKit.createTestProbe();
        cust201.tell(new Wallet.GetBalance(1,Wprobe.ref()));
        Wallet.ResponseBalance wresp = Wprobe.receiveMessage(Duration.ofSeconds(10));
        System.out.println("### Balance for the Customer 201 : " + wresp.bal);
        System.out.println("### Expected Balance  : " + (10000- allotCount*step*2*10));
        assertTrue(wresp.bal == (10000- allotCount*step*2*10) );     //need to ensure variable name balance
    }
    /*@Test
    public void testRideRequest() throws InterruptedException {

        //TEST CASE 1
        //TimeUnit.SECONDS.sleep(5);
        TestProbe<Main.Started> probe = testKit.createTestProbe();
        ActorRef<Main.Started> mainRef = testKit.spawn(Main.create(probe.getRef()), "MainActor");
        mainRef.tell(new Main.Started(false));
        assertEquals(true, probe.receiveMessage().response);
        System.out.println( Globals.cabs.get("101"));
        ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");
        System.out.println("CAB101: " + cab101);
        cab101.tell(new Cab.SignIn(10));


        // Customer 201 adds amount to his wallet
        ActorRef<Wallet.Command> walletRef = testKit.spawn(Wallet.create("201"));
        walletRef.tell(new Wallet.AddBalance(2000));

        TestProbe<Wallet.ResponseBalance> probe2 =
                testKit.createTestProbe(Wallet.ResponseBalance.class);
        walletRef.tell(new Wallet.GetBalance(1L, probe2.getRef()));

        Wallet.ResponseBalance response = probe2.receiveMessage();
        System.out.printf("Response" + response.bal);
        assertEquals(12000, response.bal);

        // Customer 201 adds Negative amount to his wallet
        walletRef.tell(new Wallet.AddBalance(-2000));
        walletRef.tell(new Wallet.GetBalance(1L, probe2.getRef()));
        Wallet.ResponseBalance response1 = probe2.receiveMessage();
        System.out.printf("NEGATIVE Response" + response.bal);
        assertEquals(12000, response1.bal);

        // Customer 201 requests a ride
        ActorRef<RideService.Command> rideService = Globals.rideService[0];
        TestProbe<RideService.RideResponse> probe1 = testKit.createTestProbe();
        rideService.tell(new RideService.RequestRide("201", 110, 100, probe1.ref()));
        RideService.RideResponse resp = probe1.receiveMessage();
        System.out.println(resp);
        assertNotEquals(java.util.Optional.of(-1L),resp.rideId) ;

        TestProbe<Cab.NumRidesResponse> resetCabProbe = testKit.createTestProbe();
        cab101.tell(new Cab.Reset(resetCabProbe.ref()));
        Cab.NumRidesResponse number = resetCabProbe.receiveMessage();
        System.out.println("Number of rides for 101: " + number.numRides);

        ActorRef<Wallet.Command> w201 = Globals.wallets.get("201");
        TestProbe<Wallet.ResponseBalance> resetWalletProbe = testKit.createTestProbe();
        w201.tell(new Wallet.Reset(0L, resetWalletProbe.ref()));
        Wallet.ResponseBalance bal = resetWalletProbe.receiveMessage();
        System.out.println("balance after reset 201: " + bal.bal);
        //TEST CASE 2


    }*/
/*
    @Test
    public void PrivateTestCase2() {

        TestProbe<Wallet.ResponseBalance> Walletprobe =
                testKit.createTestProbe(Wallet.ResponseBalance.class);
        TestProbe<Main.Started> probe = testKit.createTestProbe();
        ActorRef<Main.Started> mainRef = testKit.spawn(Main.create(probe.getRef()), "MainActor");
        mainRef.tell(new Main.Started(false));
        assertEquals(true, probe.receiveMessage().response);
        System.out.println( Globals.cabs.get("101"));

        // Cab 101 SignIn
        ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");
        cab101.tell(new Cab.SignIn(70)); // Cab 101 should signIn

        // Cab 102 SignIn
        ActorRef<Cab.Command> cab102 = Globals.cabs.get("102");
        cab102.tell(new Cab.SignIn(80)); // Cab 102 should signIn

        // Cab 103 SignIn
        ActorRef<Cab.Command> cab103 = Globals.cabs.get("103");
        cab103.tell(new Cab.SignIn(90)); // Cab 103 should signIn

        // Customer 201 requests a ride
        ActorRef<RideService.Command> rideService = Globals.rideService[0];
        TestProbe<RideService.RideResponse> probe1 = testKit.createTestProbe();
        rideService.tell(new RideService.RequestRide("201", 10, 100, probe1.ref()));
        RideService.RideResponse resp = probe1.receiveMessage();
        System.out.println(resp);
        assertNotEquals(java.util.Optional.of(-1L),resp.rideId) ;  // Ride for cust 201 should get started

        // Customer 202 requests a ride
        ActorRef<RideService.Command> rideService2 = Globals.rideService[1];
        TestProbe<RideService.RideResponse> probe2 = testKit.createTestProbe();
        rideService2.tell(new RideService.RequestRide("202", 10, 110, probe2.ref()));
        RideService.RideResponse resp2 = probe2.receiveMessage();
        System.out.println(resp2);
        assertNotEquals(java.util.Optional.of(-1L),resp2.rideId) ; // Ride for cust 202 should get started

        // Customer 203 requests a ride
        ActorRef<RideService.Command> rideService3 = Globals.rideService[5];
        TestProbe<RideService.RideResponse> probe3 = testKit.createTestProbe();
        rideService3.tell(new RideService.RequestRide("203", 10, 120, probe3.ref()));
        RideService.RideResponse resp3 = probe3.receiveMessage();
        System.out.println(resp3);
        assertNotEquals(java.util.Optional.of(-1L),resp3.rideId) ;  // Ride for cust 203 should get started

        // End Ride 1
        cab101.tell(new Cab.RideEnded(resp.rideId));  // Ride 1 should get ended

        // End Ride 2
        cab102.tell(new Cab.RideEnded(resp2.rideId));  // Ride 2 should get ended

        // End Ride 3
        cab103.tell(new Cab.RideEnded(resp3.rideId));  // Ride 3 should get ended

        // Cab 104 SignIn
        ActorRef<Cab.Command> cab104 = Globals.cabs.get("104");
        cab104.tell(new Cab.SignIn(0));  // Cab 104 should signIn

        // Customer 201 requests a ride
        ActorRef<RideService.Command> rideService4 = Globals.rideService[4];
        TestProbe<RideService.RideResponse> probe4 = testKit.createTestProbe();
        rideService4.tell(new RideService.RequestRide("201", 100, 10, probe4.ref()));
        RideService.RideResponse resp4 = probe4.receiveMessage();
        System.out.println(resp4);
        assertNotEquals(java.util.Optional.of(-1L),resp4.rideId) ;

    } */

    /*@Test

        public void PrivateTestCase3() {
            TestProbe<Wallet.ResponseBalance> probe =
                    testKit.createTestProbe(Wallet.ResponseBalance.class);

            // Cab 101 SignIn
            ActorRef<Cab.Command> cab101 = Globals.cabs.get("101");
            cab101.tell(new Cab.SignIn(70)); // Cab 101 should signIn

            // Customer 201 requests a ride from 1000 to 0
            ActorRef<RideService.Command> rideService1 = Globals.rideService[0];
            TestProbe<RideService.RideResponse> probe1 = testKit.createTestProbe();
            rideService1.tell(new RideService.RequestRide("201", 1000, 0, probe1.ref()));
            RideService.RideResponse resp1 = probe1.receiveMessage();
            assertEquals(resp1.rideId,-1);  // Ride for cust 201 should get denied

            // Check Wallet Balance for Customer 201
            ActorRef<Wallet.Command> wr = testKit.spawn(Wallet.create("201"));
            wr.tell(new Wallet.GetBalance(1L, probe.getRef()));
            Wallet.ResponseBalance response = probe.receiveMessage();
            // assertEquals(1L, response.requestId);
            assertEquals(10000, response.bal);  // Customer 201 balance should not deducted


        }*/



}