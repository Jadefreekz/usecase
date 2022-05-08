#! /bin/sh
# this test case checks allotment of cabs, checking which cab is alloted if more than
#one cab are at same distance from the customer's source location.
#Service is designed so that the cab which  signed in later at the same location will get first request

#Then checks if all three cabs are available and any incoming request is their second, then 
#they all will decline.

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
#cab 104 signs in and at position 12
resp=$(curl -s "http://localhost:8080/signIn?cabId=104&initialPos=12")
if [ "$resp" = "true" ];
then
        echo "Cab 104 signed in"
else
        echo "Cab 104 could not sign in"
        testPassed="no"
fi

#cab 102 signs in and at position 30
resp=$(curl -s "http://localhost:8080/signIn?cabId=102&initialPos=30")
if [ "$resp" = "true" ];
then
        echo "Cab 102 signed in"
else
        echo "Cab 102 could not sign in"
        testPassed="no"
fi

#customer 201 requests a ride from Location 6 to destination Location 80
#cab 104 is allocated 
rideId1=$(curl -s "http://localhost:8081/requestRide?custId=201&sourceLoc=6&destinationLoc=80")
if [ "$rideId1" != "-1" ];
then
        echo "Ride by customer 201 started with ride id "$rideId1
else
        echo "Ride to customer 201 denied"
        testPassed="no"
fi

#Since cab 104 is alloted to customer 201,
#Status of a cab on ride should be giving-ride
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=104")
if [ "$resp" != "giving-ride 6 201 80" ];
then
    echo "Invalid Status for the cab 104"
    testPassed="no"
else
    echo "Correct Status for the cab 104"
fi



#customer 202 requests a ride from Location 6 to destination Location 71
#cab 101 is allocated 
rideId2=$(curl -s "http://localhost:8081/requestRide?custId=202&sourceLoc=6&destinationLoc=71")
if [ "$rideId2" != "-1" ];
then
        echo "Ride by customer 202 started with ride id "$rideId2
else
        echo "Ride to customer 202 denied"
        testPassed="no"
fi
#Since cab 101 is alloted to customer 202,
#Status of a cab on ride should be giving-ride
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "giving-ride 6 202 71" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#customer 203 requests a ride from Location 35 to destination Location 55
#cab 102 is allocated 
rideId3=$(curl -s "http://localhost:8081/requestRide?custId=203&sourceLoc=35&destinationLoc=55")
if [ "$rideId3" != "-1" ];
then
        echo "Ride by customer 203 started with ride id "$rideId3
else
        echo "Ride to customer 203 denied"
        testPassed="no"
fi

#Since cab 102 is alloted to customer 203,
#Status of a cab on ride should be giving-ride
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
if [ "$resp" != "giving-ride 35 203 55" ];
then
    echo "Invalid Status for the cab 102"
    testPassed="no"
else
    echo "Correct Status for the cab 102"
fi

#ride for cab 104 ends
resp=$(curl -s "http://localhost:8080/rideEnded?cabId=104&rideId="$rideId1)
        #resp=$(curl -s "http://localhost:8080/rideEnded?cabId=103&rideId=1")
if [ "$resp" = "true" ];
then
    echo "Ride for cab id 104 ends"
else
    echo "Ride for the cab id 104 could not be ended"
    testPassed="no"
fi

resp=$(curl -s "http://localhost:8080/rideEnded?cabId=101&rideId="$rideId2)
        #resp=$(curl -s "http://localhost:8080/rideEnded?cabId=103&rideId=1")
if [ "$resp" = "true" ];
then
    echo "Ride for cab id 101 ends"
else
    echo "Ride for the cab id 101 could not be ended"
    testPassed="no"
fi

resp=$(curl -s "http://localhost:8080/rideEnded?cabId=102&rideId="$rideId3)
        #resp=$(curl -s "http://localhost:8080/rideEnded?cabId=103&rideId=1")
if [ "$resp" = "true" ];
then
    echo "Ride for cab id 102 ends"
else
    echo "Ride for the cab id 102 could not be ended"
    testPassed="no"
fi

#customer 201 again requests a ride from Location 6 to destination Location 50
#no cab is allocated even if all 4 are free because it probes 3 closest and they
#all decline
rideId4=$(curl -s "http://localhost:8081/requestRide?custId=201&sourceLoc=50&destinationLoc=101")
if [ "$rideId4" != "-1" ];
then
        echo "Ride by customer 202 started with ride id "$rideId4
        testPassed="no"
else
        echo "Ride to customer 202 denied because closest 3 cabs declined. All cabs are available though."
fi

#status of cab 101
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "available 71" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#status of cab 102
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
if [ "$resp" != "available 55" ];
then
    echo "Invalid Status for the cab 102"
    testPassed="no"
else
    echo "Correct Status for the cab 102"
fi


#status of cab 104
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=104")
if [ "$resp" != "available 80" ];
then
    echo "Invalid Status for the cab 104"
    testPassed="no"
else
    echo "Correct Status for the cab 104"
fi

echo "Test Passing Status: " $testPassed