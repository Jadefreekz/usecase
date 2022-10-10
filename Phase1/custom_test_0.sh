# this test case checks allotment of cabs, checking status of cab after completion of ride and sign-out
# reset RideService and Wallet.
curl -s "http://localhost:8082/reset"

curl -s "http://localhost:8081/reset"


testPassed="yes"

#cab 101 signs in and at position 0
resp=$(curl -s "http://localhost:8080/signIn?cabId=101&initialPos=0")
if [ "$resp" = "true" ];
then
        echo "Cab 101 signed in"
else
        echo "Cab 101 could not sign in"
        testPassed="no"
fi
#cab 103 signs in and at position 12
resp=$(curl -s "http://localhost:8080/signIn?cabId=103&initialPos=12")
if [ "$resp" = "true" ];
then
        echo "Cab 103 signed in"
else
        echo "Cab 103 could not sign in"
        testPassed="no"
fi
#cab 102 signs in and at position 37
resp=$(curl -s "http://localhost:8080/signIn?cabId=102&initialPos=37")
if [ "$resp" = "true" ];
then
        echo "Cab 102 signed in"
else
        echo "Cab 102 could not sign in"
        testPassed="no"
fi

#customer 201 requests a ride from Location 24 to destination Location 66
rideId=$(curl -s "http://localhost:8081/requestRide?custId=201&sourceLoc=24&destinationLoc=66")
if [ "$rideId" != "-1" ];
then
        echo "Ride by customer 201 started with ride id "$rideId
		#cabid 103 with ride id requests for rideend
        resp=$(curl -s "http://localhost:8080/rideEnded?cabId=103&rideId="$rideId)
        #resp=$(curl -s "http://localhost:8080/rideEnded?cabId=103&rideId=1")
        if [ "$resp" = "true" ];
        then
          echo "Ride for cab id 103 ends"
        else
         echo "Ride for the cab id 103 could not be ended"
         testPassed="no"
        fi
else
        echo "Ride to customer 201 denied"
        testPassed="no"
fi
#cabid 103 signOut
resp=$(curl -s "http://localhost:8080/signOut?cabId=103")
if [ "$resp" = "true" ];
then
        echo "Cab 103 Signs out"
else
        echo "cab id 103 could not sign out"
        testPassed="no"
fi

#customer 203 requests a ride from Location 40 to destination Location 22
rideId=$(curl -s "http://localhost:8081/requestRide?custId=203&sourceLoc=40&destinationLoc=22")
if [ "$rideId" != "-1" ];
then
        echo "Ride by customer 203 started with ride id "$rideId
		#cabid 102 with ride id requests for rideend
		resp=$(curl -s "http://localhost:8080/rideEnded?cabId=102&rideId="$rideId)
		if [ "$resp" = "true" ];
		then
			echo "Ride for the cab 102 ends"
		else
			echo "Ride for the cab 102 could not end"
			testPassed="no"
		fi
else
        echo "Ride to customer 201 denied"
        testPassed="no"
fi

#customer 201 requests a ride from Location 9 to destination Location 49
rideId=$(curl -s "http://localhost:8081/requestRide?custId=201&sourceLoc=9&destinationLoc=49")
if [ "$rideId" != "-1" ];
then
        echo "Ride by customer 201 started with ride id "$rideId
		#cabid 101 with ride id requests for rideend
		resp=$(curl -s "http://localhost:8080/rideEnded?cabId=101&rideId="$rideId)
		if [ "$resp" = "true" ];
		then
				echo "Ride for the cab id 101 ends"
		else
				echo "Ride for the cab id 101 could not end"
				testPassed="no"
		fi
else
        echo "Ride to customer 201 denied"
        testPassed="no"
fi


#cab status of cab id 101
cabstatus=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
echo $cabstatus
if [ "$cabstatus" = "available 49" ];
then
        echo "cab id 101 is in available state"
else
        echo "cab id 101 is in wrong state"
        testPassed="no"
fi

#cab status of cab id 102
cabstatus=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
echo $cabstatus
if [ "$cabstatus" = "available 22" ];
then
        echo "cab id 102 is in available state"
else
        echo "cab id 102 is in wrong state"$cabstatus
        testPassed="no"
fi

#cab status of cab id 103
cabstatus=$(curl -s "http://localhost:8081/getCabStatus?cabId=103")
echo $cabstatus
if [ "$cabstatus" != "signed-out -1" ];
then
        echo "Invalid status for cab 103"
        testPassed="no"
else
        echo "cab id 103 is in signed out status"
fi

echo "Test Passing Status: " $testPassed
