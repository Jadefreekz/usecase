package com.project1.Cab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;

@SpringBootApplication
@RestController
public class CabApplication {

	//Hashmap declared as public
	public static HashMap<Long,Cab> cabList = new HashMap<>();
	public static long initialBalance = 0;
	static long numberOfCabs = 0;

	static String rideServiceHomeURL = "http://172.19.0.3:8080/";
	static String walletHomeURL = "http://172.19.0.4:8080/";

	//USED @GETMAPPING INSTEAD OF @REQUESTMAPPING FOR SIMPLICITY
	// "http://localhost:8080/requestRide?cabId=110&rideId=100&sourceLoc=3&destinationLoc=4"
	//USED TO HAIL A CAB, CALLED BY RIDESERVICE.REQUESTRIDE()
	@GetMapping("/requestRide")
	public boolean requestRide(@RequestParam("cabId") long cabId, @RequestParam("rideId") long rideId,
				@RequestParam("sourceLoc") long sourceLoc, @RequestParam("destinationLoc") long destinationLoc)
	{
		if(cabList.containsKey(cabId)){ // checking that it is valid cab id or not
			Cab obj = cabList.get(cabId);
			obj.incReqNo();    // incrementing request no. of that cab
			System.out.println("Cab: " + obj.getCabId() + " request no: " + obj.getRequestNo() + "src: " + sourceLoc + "dest: " + destinationLoc);
			// checking if the cab is in available state and the request no. is alternate.
			if(obj.getState().equals("available") && obj.getRequestNo() % 2 == 1)
			{
				obj.setRideId(rideId);
				obj.setSourcePos(sourceLoc);
				obj.setDestPos(destinationLoc);
				obj.setState("committed");
				return true;
			}

		}
		return false;

	}

	@GetMapping("/rideStarted")
	public boolean rideStarted(@RequestParam("cabId") long cabId, @RequestParam("rideId") long rideId){
		if(cabList.containsKey(cabId)){  // Checking cab id  validity.
			Cab obj = cabList.get(cabId);
			if(obj.getState().equals("committed") && obj.getRideId() == rideId){
				// Checking the current state is committed and for the same ride.
				obj.incRideNo();
				obj.setRideId(rideId);
				obj.rideHistory.add(rideId);
				obj.setState("giving-ride");
				obj.setCurrPos(obj.getSourcePos());
				return true;
			}
		}
		return false;
	}

	@GetMapping("/rideCanceled")
	public boolean rideCanceled(@RequestParam("cabId") long cabId, @RequestParam("rideId") long rideId){
		if(cabList.containsKey(cabId)){  // Checking cab id  validity.
			Cab obj = cabList.get(cabId);
			if(obj.getState().equals("committed") && obj.getRideId() == rideId){ // Checking the current state is committed and for the same ride.
				obj.setState("available") ;
				obj.setSourcePos(0);
				obj.setDestPos(0);
				obj.setRideId(0);
				obj.setCurrPos(obj.getInitialPos());
				return true;
			}
		}
		return false;
	}

	@GetMapping("/rideEnded")
	public boolean rideEnded(@RequestParam("cabId") long cabId, @RequestParam("rideId") long rideId){
		if(cabList.containsKey(cabId)){  // Checking cab id  validity.
			Cab obj = cabList.get(cabId);
			if(obj.getState().equals("giving-ride") && obj.getRideId() == rideId) { // Checking the current state is committed and for the same ride.
				obj.setState("available");

				String URL = rideServiceHomeURL + "rideEnded?cabId=" + obj.getCabId() + "&rideId="+
						obj.getRideId();
				//try Cab.rideRequest
				try{
					var request = HttpRequest.newBuilder().GET().uri(URI.create(URL)).build();
					var client = HttpClient.newBuilder().build();
					var response = client.send(request, HttpResponse.BodyHandlers.ofString());
					/*if(Boolean.valueOf(response.body()))//if response is true
					{

					}*/
				}
				catch (Exception e)
				{
					System.out.println("Cannot Reach "+ URL );
					e.printStackTrace();
				}
				obj.setCurrPos(obj.getDestPos());
				return true;
			}
		}
		return false;
	}

	@GetMapping("/signIn")
	public boolean signIn(@RequestParam("cabId") long cabId, @RequestParam("initialPos") long initialPos){
		if(cabList.containsKey(cabId)){  // Checking cab id  validity.
			Cab obj = cabList.get(cabId);
			if(obj.getState().equals("signed-out")){ // If the current state is signOut.
				String URL = rideServiceHomeURL + "cabSignsIn?cabId=" + obj.getCabId() + "&initialPos=" + initialPos ;
//try Cab.rideRequest
				try{
					//http request
					var request = HttpRequest.newBuilder().GET().uri(URI.create(URL)).build();
					var client = HttpClient.newBuilder().build();

					var response = client.send(request, HttpResponse.BodyHandlers.ofString());

					if(Boolean.valueOf(response.body()))//if response is true
					{
						obj.setState("available");
						obj.setInitialPos(initialPos);

						return true;
					}
				}
				catch (Exception e)
				{
					System.out.println("Cannot Reach "+ URL );
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@GetMapping("/signOut")
	public  boolean signOut(@RequestParam("cabId") long cabId){
		if(cabList.containsKey(cabId)){  // Checking cab id  validity.
			Cab obj = cabList.get(cabId);
			if(!obj.getState().equals("signed-out")){ //If the current state is signed in.
				String URL = rideServiceHomeURL + "cabSignsOut?cabId=" + obj.getCabId();
//try Cab.rideRequest
				try{
					//http request
					var request = HttpRequest.newBuilder().GET().uri(URI.create(URL)).build();
					var client = HttpClient.newBuilder().build();

					var response = client.send(request, HttpResponse.BodyHandlers.ofString());

					if(Boolean.valueOf(response.body()))//if response is true
					{
						obj.setState("signed-out");
						obj.setRideZero();
						obj.setSourcePos(-1);
						obj.setDestPos(-1);
						obj.setRideId(-1);
						obj.setRequestNo(0);

						if(obj.getNumRides() >= 1)
						{
							obj.setInitialPos(obj.getDestPos());
							obj.setCurrPos(obj.getDestPos());
						}

						return true;
					}
				}
				catch (Exception e)
				{
					System.out.println("Cannot Reach "+ URL );
					e.printStackTrace();
				}
			}
		}
		return false;
	}

	@GetMapping("/numRides")
	public int numRides(@RequestParam("cabId") int cabId){
		if(cabList.containsKey(cabId)==false) {// Invalid cabID
			return -1;
		}
		else{
			Cab obj = cabList.get(cabId);
			if(obj.getState().equals("signed-in") ){ // Checking the current state is signed-in.
				return obj.getNumRides();
			}
			else{
				return 0;
			}
		}
	}

	@GetMapping("/getCabStatus")
	public String getCabStatus(@RequestParam("cabId") long cabId)
	{
		Cab c = cabList.get(cabId);

		if(c.getState().equals("giving-ride"))
		{
			return "CabId: " + cabId + ", Status: "+ c.getState() + ", CustId: " + c.getCustId() +
					", Last known location: " + c.getCurrPos()+", Destination: " + c.getDestPos();
		}
		else if (c.getState().equals("committed") && !c.rideHistory.isEmpty())
		{
			long lastRide = c.rideHistory.get(c.rideHistory.size()-1);
			return "CabId: " + cabId + ", Status: "+ c.getState() + ", Last known location: "
					+ c.getDestPos();
		}
		else if (c.getState().equals("available") && !c.rideHistory.isEmpty())
		{
			long lastRide = c.rideHistory.get(c.rideHistory.size()-1);
			return "CabId: " + cabId + ", Status: "+ c.getState() + ", Last known location: "
					+ c.getDestPos();
		}
		else if (c.getState().equals("available") && c.rideHistory.isEmpty())
		{
			return "CabId: " + cabId + ", Status: "+ c.getState() + ", Last known location: "
					+ c.getInitialPos();
		}
		return "CabId: " + cabId + ", Status: "+ c.getState() + ", Last known location: " + -1;
	}


	@GetMapping("/hello")
	public String sayHello(@RequestParam("id") long id)
	{
		return "Hello to id+1: "+ (id+1);
	}

	public static void main(String[] args) {

		//Read file and details
		try {
			ArrayList<Long> cabs = new ArrayList<Long>();
			//ArrayList<Long> cust = new ArrayList<Long>();
			long i = 0;

			File myObj = new File("/home/IDs.txt");
			Scanner myReader = new Scanner(myObj);
			while (myReader.hasNextLine()) {
				String data = myReader.nextLine();
				if (data.contains("****")) {
					i++;
					continue;
				}
				if (i == 1) {
					numberOfCabs++;
					long cabID = Long.parseLong(data);
					cabs.add(cabID);
				}
				/*if(i == 2)
				{
					long custID = Long.parseLong(data);
					cust.add(custID);
				}*/
				if (i == 3) {
					initialBalance = Long.parseLong(data);

				}
			}

			myReader.close();

			//Creating cab ojects from read cabIds and putting them in HashMap
			for(Long a : cabs)
			{
				//Check if source and destination is needed in Customer Class
				Cab c = new Cab(a, 0, 0, 0, "Signed Out");
				cabList.put(a, c);
			}

			for(Long key : cabList.keySet())
			{
				System.out.println("Cab ID: " + cabList.get(key).getCabId() +" State: " + cabList.get(key).getState());
			}
		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}

		SpringApplication.run(CabApplication.class, args);
	}
}
