#! /bin/sh
# this test case checks, when customer does not have enough balance, but cab accepts request, so status
# goes from available to committed to available again.

# reset RideService and Wallet.
# every test case should begin with these two steps
curl -s http://localhost:8081/reset
curl -s http://localhost:8082/reset

testPassed="yes"

#Step 1 : Status of a signed-out cab
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "signed-out -1" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#Step 2 : cab 101 signs in
resp=$(curl -s "http://localhost:8080/signIn?cabId=101&initialPos=100")
if [ "$resp" = "true" ];
then
    echo "Cab 101 signed in"
else
    echo "Cab 101 could not sign in"
    testPassed="no"
fi

#Step 3 : Status of a signed-in cab
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "available 100" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

#Step 4 : customer 201 requests a ride . The state of can will go from available to committed and then again to available.\\
rideId=$(curl -s "http://localhost:8081/requestRide?custId=201&sourceLoc=110&destinationLoc=2000")
if [ "$rideId" == "-1" ];
then
    echo "Ride rejected because customer 201 doestn't have enough balance"
else
    echo "Ride to customer 201 accepted"
    testPassed="no"
fi

#Step 5 : Status of a signed-in cab
resp=$(curl -s "http://localhost:8081/getCabStatus?cabId=101")
if [ "$resp" != "available 100" ];
then
    echo "Invalid Status for the cab 101"
    testPassed="no"
else
    echo "Correct Status for the cab 101"
fi

echo "Test Passing Status: " $testPassed
