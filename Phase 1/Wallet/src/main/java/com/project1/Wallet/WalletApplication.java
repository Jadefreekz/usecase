package com.project1.Wallet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;


@SpringBootApplication
@RestController
public class WalletApplication {
	public static HashMap<Long, Customer> customerList = new HashMap<>();
	public static long initialBalance = 0;

	//Get Customer Balance
	@GetMapping("/getBalance")
	public long getBalance(@RequestParam("custId") long custId)
	{
		long balance = -1;
		balance = customerList.get(custId).getWallet();
		return balance ;
	}

	//Deduct amount from customer wallet
	@GetMapping("/deductAmount")
	public boolean deductAmount(@RequestParam("custId") long custId, @RequestParam("amount") long amount)
	{
		Customer c = customerList.get(custId);
		long balance = c.getWallet();
		if(balance >= amount)
		{
			c.setWallet(balance - amount);
			return true;
		}
		else
		{
			return false;
		}
	}

	//Add amount to wallet
	@GetMapping("/addAmount")
	public boolean addAmount(@RequestParam("custId") long custId, @RequestParam("amount") long amount)
	{
		try {
			Customer c = customerList.get(custId);
			c.setWallet(c.getWallet() + amount);
		}
		catch (Exception e)
		{
			return false;
		}
		return true;
	}
	@GetMapping("/reset")
	public void reset()
	{
		for(Long key : customerList.keySet())
		{
			customerList.get(key).setWallet(initialBalance);
		}
	}
	public static void main(String[] args) {
		//Read file and details
		try {
			//ArrayList<Long> cabs = new ArrayList<Long>();
			ArrayList<Long> cust = new ArrayList<Long>();
			long i = 0;

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
				/*if(i == 1)
				{
					long cabID = Long.parseLong(data);
					//cabs.add(cabID);
				}*/
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

			//Add the read values to a Customer object and add it to HashMap
			for (Long a : cust) {
				//Ask if source and destination is needed in Customer Class
				Customer c = new Customer(a, 0, 0, initialBalance);
				customerList.put(a, c);

			}

		} catch (FileNotFoundException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	SpringApplication.run(WalletApplication.class, args);
	}

}
