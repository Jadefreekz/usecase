package com.project1.RideService;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.*;
import java.lang.Math;

/*class cabComparator implements Comparator<Cab> {
	public int compare(Cab c1, Cab c2)
	{
		return Long.compare(c1.getCabId(), c2.getCabId());
	}
}*/

@SpringBootApplication
@RestController
public class RideServiceApplication {

	public static HashMap<Long, Customer> customerList = new HashMap<>();
	public static HashMap<Long,Cab> cabList = new HashMap<>();
	public static HashMap<Long, Ride> rideList = new HashMap<>();
	//public static PriorityQueue<Cab> availableCabQueue = new PriorityQueue<Cab>(5, new cabComparator());
	public static HashMap<Long,Cab> availableCabList = new HashMap<>();
	public static long initialBalance = 0;

	static String cabHomeURL = "http://172.19.0.2:8080/";
	static String walletHomeURL = "http://172.19.0.4:8080/";
	private static final AtomicInteger counter = new AtomicInteger(1001);

	//Homepage for Cab service
	@RequestMapping("/")
	public String home() {
		InetAddress ip = null;
		try {
			ip = InetAddress.getLocalHost();

		} catch (Exception e) {
			return "Error accessing server at : "+ ip + ".";
		}
		return "Hello Docker World. Running at : "+ ip + ".";
	}//END OF home()

	@GetMapping("/rideEnded")
	public boolean rideEnded(@RequestParam("rideId") long rideId)
	{
		if(rideList.containsKey(rideId) && rideList.get(rideId).isRideOngoing())
		{
			Ride r = rideList.get(rideId);
			Cab c = cabList.get(r.getCabID());
			r.setIsOngoing(false);
			c.setState("available");
			c.setInitialPos(c.getDestPos());
			c.setCurrPos(c.getDestPos());
			availableCabList.put(c.getCabId(),c);
			return true;
		}
		return false;
	}

	@GetMapping("/cabSignsIn")
	public  boolean cabSignsIn(@RequestParam("cabId") long cabId,
							   @RequestParam("initialPos") long initialPos)
	{
		Cab c = cabList.get(cabId);
		if(cabList.containsKey(cabId) && c.getState().equals("signed-out")) {

			c.setState("available");
			c.setInitialPos(initialPos);
			c.setCurrPos(initialPos);
			availableCabList.put(c.getCabId(), c);
			return true;
		}
		return false;
	}

	@GetMapping("/cabSignsOut")
	public boolean cabSignsOut(@RequestParam("cabId") long cabId)
	{

		if(cabList.containsKey(cabId) && cabList.get(cabId).getState().equals("available"))
		{
			if(!availableCabList.isEmpty() && availableCabList.containsKey(cabId))
				availableCabList.remove(cabId);
			Cab c = cabList.get(cabId);
			c.setState("signed-out");
			return true;
		}
		return false;
	}
	// "http://localhost:8080/requestRide?cabId=110&rideId=100&sourceLoc=3&destinationLoc=4"
	@GetMapping("/requestRide")
	public long requestRide(@RequestParam("custId") long custId,
						   @RequestParam("sourceLoc") long sourceLoc,
						   @RequestParam("destinationLoc") long destinationLoc)
	{
		long rideId = counter.getAndIncrement();
		long searchCounter = 0;
		Cab acceptedCab;

		while(searchCounter <3 )
		{
			Cab temp1 = availableCabList.get(getClosestCab(sourceLoc));
			if(temp1 == null)
				return  -1;
			Cab c1 = cabList.get(temp1.getCabId());
			searchCounter++;

			String CabRideURL = cabHomeURL + "requestRide?cabId=" + c1.getCabId() + "&rideId="+
					rideId+"&sourceLoc="+sourceLoc+"&destinationLoc="+destinationLoc;
			c1.incReqNo();
			//try Cab.rideRequest
			try{
				var request = HttpRequest.newBuilder().GET().uri(URI.create(CabRideURL)).build();
				var client = HttpClient.newBuilder().build();
				var response = client.send(request, HttpResponse.BodyHandlers.ofString());
				if(Boolean.valueOf(response.body()))//IF FIRST CAB ACCEPTS REQUEST
				{
					c1.setState("committed");
					long fare = (Math.abs(c1.getCurrPos() - sourceLoc) + Math.abs(sourceLoc - destinationLoc)) * 10;
					String fareURL = walletHomeURL + "deductAmount?custId=" + custId + "&amount=" + fare;
					try {//TRY DEDUCTING FARE AMOUNT
						var requestWallet =
								HttpRequest.newBuilder().GET().uri(URI.create(fareURL)).build();
						var clientWallet =
								HttpClient.newBuilder().build();
						var responseWallet =
								clientWallet.send(requestWallet, HttpResponse.BodyHandlers.ofString());

						if(Boolean.valueOf(responseWallet.body()))//IF deduction gives SUCCESS, CALL CAB.RIDESTARTED()
						{
							String rideStartURL = cabHomeURL +"rideStarted?cabId="+c1.getCabId()+"&rideId="+rideId;
							var startReq =
									HttpRequest.newBuilder().GET().uri(URI.create(rideStartURL)).build();
							var startClient =
									HttpClient.newBuilder().build();
							var startResponse =
									startClient.send(startReq, HttpResponse.BodyHandlers.ofString());
							if(Boolean.valueOf(startResponse.body()))//CHECK RIDESTARTED() RESPONSE
							{
								//Update Customer Details
								Cab garbage = availableCabList.remove(c1.getCabId());
								Customer c = customerList.get(custId);
								long balance = c.getWallet();
								c.setWallet(balance - fare);
								c.setSrc(sourceLoc);
								c.setDest(destinationLoc);
								c1.setCustId(custId);
								c1.rideHistory.add(rideId);
								c1.setRideId(rideId);
								c1.setState("giving-ride");
								c1.setCurrPos(sourceLoc);
								c1.setSourcePos(sourceLoc);
								c1.setDestPos(destinationLoc);
								cabList.replace(c1.getCabId(), c1);
								Ride r = new Ride(rideId, c1.getCabId(), sourceLoc, destinationLoc, true);
								rideList.put(rideId, r);
								return rideId;
							}
						}
						else
						{
							String rideCancelURL = cabHomeURL + "rideCanceled?cabId=" + c1.getCabId()
									+ "&rideId=" + rideId;
							var cancelRequest = HttpRequest.newBuilder().GET().uri(URI.create(rideCancelURL)).build();
							var cancelClient = HttpClient.newBuilder().build();
							var cancelResponse = cancelClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
							if(Boolean.valueOf(cancelResponse.body()))
							{
								c1.setState("available");
								availableCabList.replace(c1.getCabId(), c1);
								return -1;
							}
						}
					}
					catch (Exception e)
					{
						System.out.println("Cannot Reach "+fareURL );
						e.printStackTrace();
					}
					return rideId;
				}
				else//Try second closest
				{
					Cab firstAvailable = availableCabList.remove(getClosestCab(sourceLoc));
					//to get second closest remove first closest
					Cab temp2 = availableCabList.get(getClosestCab(sourceLoc));
					if(temp2 == null)
					{
						availableCabList.put(firstAvailable.getCabId(),firstAvailable);
						return  -1;
					}

					Cab c2 = cabList.get(temp2.getCabId());
					searchCounter++;
					assert c2 != null;
					String CabRideURL2 = cabHomeURL + "requestRide?cabId=" + c2.getCabId() + "&rideId="+
							rideId+"&sourceLoc="+sourceLoc+"&destinationLoc="+destinationLoc;
					c2.incReqNo();
					var request2 = HttpRequest.newBuilder().GET().uri(URI.create(CabRideURL2)).build();
					var client2 = HttpClient.newBuilder().build();
					var response2 = client2.send(request2, HttpResponse.BodyHandlers.ofString());

					if(Boolean.valueOf(response2.body()))//IF SECOND CAB ACCEPTS REQUEST
					{

						c2.setState("committed");
						long fare = (Math.abs(c2.getCurrPos() - sourceLoc) + Math.abs(sourceLoc - destinationLoc)) * 10;
						String fareURL = walletHomeURL + "deductAmount?custId=" + custId + "&amount=" + fare;
						try {//TRY DEDUCTING FARE AMOUNT
							var requestWallet =
									HttpRequest.newBuilder().GET().uri(URI.create(fareURL)).build();
							var clientWallet =
									HttpClient.newBuilder().build();
							var responseWallet =
									clientWallet.send(requestWallet, HttpResponse.BodyHandlers.ofString());

							if(Boolean.valueOf(responseWallet.body()))//IF deduction gives SUCCESS, CALL CAB.RIDESTARTED()
							{
								String rideStartURL = cabHomeURL +"rideStarted?cabId="+c2.getCabId()+"&rideId="+rideId;
								var startReq =
										HttpRequest.newBuilder().GET().uri(URI.create(rideStartURL)).build();
								var startClient =
										HttpClient.newBuilder().build();
								var startResponse =
										startClient.send(startReq, HttpResponse.BodyHandlers.ofString());
								if(Boolean.valueOf(startResponse.body()))//CHECK RIDESTARTED() RESPONSE
								{
									//Update Customer Details
									//put first closest back in list
									availableCabList.put(firstAvailable.getCabId(), firstAvailable);
									Cab garbage = availableCabList.remove(c2.getCabId());
									Customer c = customerList.get(custId);
									long balance = c.getWallet();
									c.setWallet(balance - fare);
									c.setSrc(sourceLoc);
									c.setDest(destinationLoc);

									c2.setCustId(custId);
									c2.rideHistory.add(rideId);
									c2.setRideId(rideId);
									c2.setState("giving-ride");
									c2.setCurrPos(sourceLoc);
									c2.setSourcePos(sourceLoc);
									c2.setDestPos(destinationLoc);
									cabList.replace(c2.getCabId(), c2);
									Ride r = new Ride(rideId, c2.getCabId(), sourceLoc, destinationLoc, true);
									rideList.put(rideId, r);
									return rideId;
								}
							}
							else
							{
								String rideCancelURL = cabHomeURL + "rideCanceled?cabId=" + c2.getCabId()
										+ "&rideId=" + rideId;
								var cancelRequest = HttpRequest.newBuilder().GET().uri(URI.create(rideCancelURL)).build();
								var cancelClient = HttpClient.newBuilder().build();
								var cancelResponse = cancelClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
								if(Boolean.valueOf(cancelResponse.body()))
								{
									c2.setState("available");
									availableCabList.put(firstAvailable.getCabId(),firstAvailable);
									availableCabList.replace(c2.getCabId(), c2);
									cabList.replace(c2.getCabId(), c2);
									return -1;
								}
							}
						}
						catch (Exception e)
						{
							System.out.println("Cannot Reach "+fareURL );
							e.printStackTrace();
						}
						return rideId;
					}
					else
					{
						Cab secondAvailable = availableCabList.remove(getClosestCab(sourceLoc));
						Cab temp3 = availableCabList.get(getClosestCab(sourceLoc));
						if(temp3 == null)
						{
							availableCabList.put(firstAvailable.getCabId(), firstAvailable);
							availableCabList.put(secondAvailable.getCabId(), secondAvailable);
							return  -1;
						}

						Cab c3 = cabList.get(temp3.getCabId());
						searchCounter++;
						assert c3 != null;
						String CabRideURL3 = cabHomeURL + "requestRide?cabId=" + c3.getCabId() + "&rideId="+
								rideId+"&sourceLoc="+sourceLoc+"&destinationLoc="+destinationLoc;
						c3.incReqNo();
						var request3 = HttpRequest.newBuilder().GET().uri(URI.create(CabRideURL3)).build();
						var client3 = HttpClient.newBuilder().build();
						var response3 = client3.send(request3, HttpResponse.BodyHandlers.ofString());
						if(Boolean.valueOf(response3.body()))//IF THIRD CAB ACCEPTS REQUEST
						{

							c3.setState("committed");
							long fare = (Math.abs(c3.getCurrPos() - sourceLoc) + Math.abs(sourceLoc - destinationLoc)) * 10;
							String fareURL = walletHomeURL + "deductAmount?custId=" + custId + "&amount=" + fare;
							try {//TRY DEDUCTING FARE AMOUNT
								var requestWallet =
										HttpRequest.newBuilder().GET().uri(URI.create(fareURL)).build();
								var clientWallet =
										HttpClient.newBuilder().build();
								var responseWallet =
										clientWallet.send(requestWallet, HttpResponse.BodyHandlers.ofString());

								if(Boolean.valueOf(responseWallet.body()))//IF deduction gives SUCCESS, CALL CAB.RIDESTARTED()
								{
									String rideStartURL = cabHomeURL +"rideStarted?cabId="+c3.getCabId()+"&rideId="+rideId;
									var startReq =
											HttpRequest.newBuilder().GET().uri(URI.create(rideStartURL)).build();
									var startClient =
											HttpClient.newBuilder().build();
									var startResponse =
											startClient.send(startReq, HttpResponse.BodyHandlers.ofString());
									if(Boolean.valueOf(startResponse.body()))//CHECK RIDESTARTED() RESPONSE
									{
										//Update Customer Details
										//Put first and second available back in list
										availableCabList.put(firstAvailable.getCabId(), firstAvailable);
										availableCabList.put(secondAvailable.getCabId(), secondAvailable);
										Cab garbage = availableCabList.remove(c3.getCabId());
										Customer c = customerList.get(custId);
										long balance = c.getWallet();
										c.setWallet(balance - fare);
										c.setSrc(sourceLoc);
										c.setDest(destinationLoc);

										c3.setCustId(custId);
										c3.rideHistory.add(rideId);
										c3.setRideId(rideId);
										c3.setState("giving-ride");
										c3.setCurrPos(sourceLoc);
										c3.setSourcePos(sourceLoc);
										c3.setDestPos(destinationLoc);
										cabList.replace(c3.getCabId(), c3);

										Ride r = new Ride(rideId, c3.getCabId(), sourceLoc, destinationLoc, true);
										rideList.put(rideId, r);
										return rideId;
									}
								}
								else
								{
									String rideCancelURL = cabHomeURL + "rideCanceled?cabId=" + c3.getCabId()
											+ "&rideId=" + rideId;
									var cancelRequest = HttpRequest.newBuilder().GET().uri(URI.create(rideCancelURL)).build();
									var cancelClient = HttpClient.newBuilder().build();
									var cancelResponse = cancelClient.send(cancelRequest, HttpResponse.BodyHandlers.ofString());
									if(Boolean.valueOf(cancelResponse.body()))
									{
										availableCabList.put(firstAvailable.getCabId(), firstAvailable);
										availableCabList.put(secondAvailable.getCabId(), secondAvailable);
										c3.setState("available");
										availableCabList.replace(c3.getCabId(), c3);
										return -1;
									}
								}
							}
							catch (Exception e)
							{
								System.out.println("Cannot Reach "+fareURL );
								e.printStackTrace();
							}
							return rideId;
						}

					}

				}

			}catch (Exception e)
			{
				System.out.println("Cannot Reach "+cabHomeURL );
				e.printStackTrace();
			}
		}

		return -1;
	}
	@GetMapping("/availableCabs")
	public void availableCabs()
	{
		System.out.println("READY Cabs: ");
		if(!availableCabList.isEmpty()){
			for (long c : availableCabList.keySet()) {
				System.out.println("Cabs available: " + availableCabList.get(c).getCabId());
			}
		}
	}
	@GetMapping("/getCabStatus")
	public String getCabStatus(@RequestParam("cabId") long cabId)
	{
		Cab c = cabList.get(cabId);
		Ride r = rideList.get(c.getRideId());
		if(c.getState().equals("giving-ride"))
		{
			return c.getState() + " " + r.getSourceLoc() + " " + c.getCustId() +
					" " + r.getDestinationLoc();
		}
		else if (c.getState().equals("committed") && !c.rideHistory.isEmpty())
		{
			long lastRide = c.rideHistory.get(c.rideHistory.size()-1);
			return c.getState() + " " + rideList.get(lastRide).getDestinationLoc();
		}
		else if (c.getState().equals("available") && !c.rideHistory.isEmpty())
		{
			long lastRide = c.rideHistory.get(c.rideHistory.size()-1);
			return c.getState() + " " + rideList.get(lastRide).getDestinationLoc();
		}
		else if (c.getState().equals("available") && c.rideHistory.isEmpty())
		{
			return c.getState() + " " + c.getInitialPos();
		}
		return c.getState() + " " + -1;
	}
	@GetMapping("/reset")
	public void reset()
	{
		String walletReset = walletHomeURL + "reset";

		try{
			var request = HttpRequest.newBuilder().GET().uri(URI.create(walletReset)).build();
			var client = HttpClient.newBuilder().build();
			var response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if(Boolean.valueOf(response.body()))
			{
				System.out.println("wallet Reset");
			}
			for(Long l : cabList.keySet())
			{
				Cab c = cabList.get(l);
				if(c.getState().equals("giving-ride"))
				{
					String rideEnded = cabHomeURL + "rideEnded/?cabId=" + c.getCabId() + "&rideId="
							+ c.getRideId();
					request = HttpRequest.newBuilder().GET().uri(URI.create(rideEnded)).build();
					client = HttpClient.newBuilder().build();
					response = client.send(request, HttpResponse.BodyHandlers.ofString());
					if(Boolean.valueOf(response.body()))
					{
						String signOut = cabHomeURL + "signOut?cabId=" + c.getCabId();
						request = HttpRequest.newBuilder().GET().uri(URI.create(signOut)).build();
						client = HttpClient.newBuilder().build();
						response = client.send(request, HttpResponse.BodyHandlers.ofString());
						if(Boolean.valueOf(response.body()))
						{

							c.setInitialPos(-1);
							c.setCurrPos(-1);
							c.setSourcePos(-1);
							c.setDestPos(-1);
							c.setRideId(-1);
							c.setRequestNo(0);
							c.setState("signed-out");
							c.rideHistory.clear();
							System.out.println("Cab "+c.getCabId()+" Signed out.");
						}

					}
				}
				else if(c.getState().equals("available"))
				{
					String signOut = cabHomeURL + "signOut?cabId=" + c.getCabId();
					request = HttpRequest.newBuilder().GET().uri(URI.create(signOut)).build();
					client = HttpClient.newBuilder().build();
					response = client.send(request, HttpResponse.BodyHandlers.ofString());
					if(Boolean.valueOf(response.body()))
					{

						c.setInitialPos(-1);
						c.setCurrPos(-1);
						c.setSourcePos(-1);
						c.setDestPos(-1);
						c.setRideId(-1);
						c.setRequestNo(0);
						c.setState("signed-out");
						c.rideHistory.clear();
						System.out.println("Cab "+c.getCabId()+" Signed out.");
					}

				}
			}
			availableCabList.clear();
		}catch (Exception e)
		{
			System.out.println("Cannot Reset");
			e.printStackTrace();
		}

	}

	public long getClosestCab(long sourceLoc)
	{
		long min = Long.MAX_VALUE;
		long closestCab = 0;
		for(Long l : availableCabList.keySet())
		{
			Cab c = availableCabList.get(l);
			if(Math.abs(c.getCurrPos() - sourceLoc) <= min)
			{
				min = Math.abs(c.getCurrPos() - sourceLoc);
				closestCab = c.getCabId();
			}
		}
		return closestCab;
	}

	public static void main(String[] args) {
		//Read file and details
		try {
			ArrayList<Long> cabs = new ArrayList<Long>();
			ArrayList<Long> cust = new ArrayList<Long>();
			int i = 0;

			File myObj = new File("/home/IDs.txt");
			Scanner myReader = new Scanner(myObj);
			while (myReader.hasNextLine())
			{
				String data = myReader.nextLine();
				if (data.contains("****"))
				{
					i++;
					continue;
				}
				if(i == 1)
				{
					long cabID = Long.parseLong(data);
					cabs.add(cabID);
				}
				if(i == 2)
				{
					long custID = Long.parseLong(data);
					cust.add(custID);
				}
				if(i == 3)
				{
					initialBalance = Long.parseLong(data);

				}
			}

			myReader.close();

			//Creating cab objects from read cabIds and putting them in HashMap
			for(Long a : cabs)
			{
				//Check if source and destination is needed in Customer Class
				Cab c = new Cab(a, 0, 0, 0, "Signed Out");
				cabList.put(a, c);
			}


			//Add the read values to a Customer object and add it to HashMap
			for (Long a : cust) {
				//Ask if source and destination is needed in Customer Class
				Customer c = new Customer(a, 0, 0, initialBalance);
				customerList.put(a, c);

			}
			for(long l : cabList.keySet())
			{
				System.out.println("Cab: "+cabList.get(l).getCabId()+ " " + cabList.get(l).getState());

			}

			for(Long a : customerList.keySet())
			{
				System.out.println("Cab: "+customerList.get(a).getCustId()+ " " + customerList.get(a).getWallet());
			}

		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
		SpringApplication.run(RideServiceApplication.class, args);
	}

}
