#! /bin/sh
# This test case checks the following:
# For ex:  2 cabs sign in, 1st cab after giving ride went to available state,
# 2nd request goes to 2nd cab even though 1st cab is closer and in available state.

# every test case should begin with these two steps
curl -s http://localhost:8081/reset
curl -s http://localhost:8082/reset

testPassed="yes"

#Step 1 : cab 101 signs in
resp=$(curl -s "http://localhost:8080/signIn?cabId=101&initialPos=100")
if [ "$resp" = "true" ];
then
    echo "Cab 101 signed in"
else
    echo "Cab 101 could not sign in"
    testPassed="no"
fi


#Step 2 : Status of a signed-in cab
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "available 100" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#Step 3 : cab 102 signs in
resp=$(curl -s "http://localhost:8080/signIn?cabId=102&initialPos=200")
if [ "$resp" = "true" ];
then
    echo "Cab 102 signed in"
else
    echo "Cab 102 could not sign in"
    testPassed="no"
fi

#Step 4 : Status of a signed-in cab
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
if [ "$resp" != "available 200" ];
then
    echo "Invalid Status for the cab 102"
    testPassed="no"
else
    echo "Correct Status for the cab 102"
fi

#Step 5 : customer 201 requests a ride
rideId=$(curl -s "http://localhost:8081/requestRide?custId=201&sourceLoc=100&destinationLoc=160")
if [ "$rideId" != "-1" ];
then
    echo "Ride by customer 201 started"
else
    echo "Ride to customer 201 denied"
    testPassed="no"
fi

#Step 6 : Status of a cab on ride
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "giving-ride 100 201 160" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#Step 7 : End ride1
resp=$(curl -s "http://localhost:8080/rideEnded?cabId=101&rideId=$rideId")
if [ "$resp" = "true" ];
then
    echo $rideId " has ended"
else
    echo "Could not end" $rideId
    testPassed="no"
fi

#Step 8 : #Status of a cab 101 after a ride
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "available 160" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#Step 9 : Status of a signed-in cab 102
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
if [ "$resp" != "available 200" ];
then
    echo "Invalid Status for the cab 102"
    testPassed="no"
else
    echo "Correct Status for the cab 102"
fi

#Step 10 : customer 202 requests a ride
rideId=$(curl -s "http://localhost:8081/requestRide?custId=202&sourceLoc=165&destinationLoc=210")
if [ "$rideId" != "-1" ];
then
    echo "Ride by customer 201 started"
else
    echo "Ride to customer 201 denied"
    testPassed="no"
fi

#Step 11 : #Status of a cab 101
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "available 160" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#Step 12 : Status of a signed-in cab 102
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
if [ "$resp" != "giving-ride 165 202 210" ];
then
    echo "Invalid Status for the cab 102"
    testPassed="no"
else
    echo "Correct Status for the cab 102"
fi

#Step 13 : End ride2
resp=$(curl -s "http://localhost:8080/rideEnded?cabId=102&rideId=$rideId")
if [ "$resp" = "true" ];
then
    echo $rideId " has ended"
else
    echo "Could not end" $rideId
    testPassed="no"
fi

#Step 14 : #Status of a cab 102
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=102")
if [ "$resp" != "available 210" ];
then
    echo "Invalid Status for the cab 102"
    testPassed="no"
else
    echo "Correct Status for the cab 102"
fi

echo "Test Passing Status: " $testPassed
