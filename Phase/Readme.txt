********Requires java 11 or later*************
********not supported for older versions*****************
********Docker file in each container source folder mentions java version*******
********For Ex; "openjdk:15-jdk" is used in our docker file.********
********You may change it to other version, specifically the one matching in your system********
********Again, this version must be java 11 or later****************

*******Follow the steps below to run the three services in a container*********


//Create a custom network for docker containers
    docker network create --driver bridge my-network --subnet 172.19.0.0/16 --gateway 172.19.0.1

//Build and run the following three containers(preferably in 3 seperate terminals)
    docker build -t ride . //build RideService image
    docker build -t cab . //build cab service image
    docker build -t wallet  . //build wallet service image

*******Put IDs.txt in same directory as dockerfile, run docker command from there
*******For example run the cab container while the present working directory is the "Cab" folder, run the rideService container while in "RideService" folder, and run the wallet container while in "Wallet" folder.

//run cab service container
docker run --name cab-service --ip 172.19.0.2 -p 127.0.0.1:8080:8080 --network my-network --mount type=bind,source="$(pwd)"/IDs.txt,target=/home/IDs.txt cab

//Wallet service run
docker run --name wallet-service --ip 172.19.0.4 -p 127.0.0.1:8082:8080 --network my-network --mount type=bind,source="$(pwd)"/IDs.txt,target=/home/IDs.txt wallet

//run Riderservice container
docker run --name ride-service --ip 172.19.0.3 -p 127.0.0.1:8081:8080 --network my-network --mount type=bind,source="$(pwd)"/IDs.txt,target=/home/IDs.txt ride

********Run the testcases as follows 
bash <filename>.sh

