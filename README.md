# The Benchmark Deployment: Mongo Cluster and Redis Coordinator on Docker Swarm; Docker-Compose to Scale Baqend's REST Servers and the Benchmark Client

## Create the Swarm Cluster

### Server Image
Create a small ubuntu instance, connect via ssh and install docker:

```
#!/bin/bash

#update ubuntu (keep current version of grub while upgrading!!)
sudo apt-get update && sudo apt-get upgrade -y

#Enabling Enhanced Networking on Linux
#skipped for now, current version is not compatible with ubuntu 14.04

#install docker
sudo apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D

sudo sh -c "echo \"deb https://apt.dockerproject.org/repo ubuntu-trusty main\" > /etc/apt/sources.list.d/docker.list"
sudo apt-get update
sudo apt-cache policy docker-engine

sudo apt-get install -y linux-image-extra-$(uname -r)
sudo apt-get install -y docker-engine

#Stop docker deamon and ensure that the key file will be regenerated on each machine
sudo service docker stop
sudo rm /etc/docker/key.json

#Add the ubuntu user to the docker group
sudo usermod -aG docker ubuntu
```
Take a snapshot of the maschine and terminate the isntance.

### Swarm Worker
Create 5 big servers as swarm workers from the image and usethe following start script (replace the server numbers) and make sure the servers can communicate on ports 2181, 2888, 3888 and 2375:
```
#!/bin/bash
# first script argument: the servers in the ZooKeeper ensemble:
ZOOKEEPER_SERVERS="zk1.os.baqend.com,zk2.os.baqend.com,zk3.os.baqend.com"

# second script argument: the role of this node:
# ("manager" for the Swarm manager node; leave empty else)
LABELS="--label server=worker --label az=server1"

# the IP address of this machine:
PRIVATE_IP=$(/sbin/ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}')

# define default options for Docker Swarm:
echo "DOCKER_OPTS=\"-H tcp://0.0.0.0:2375 \
    -H unix:///var/run/docker.sock \
    --cluster-advertise eth0:2375 \
    $LABELS \
    --cluster-store \
    zk://$ZOOKEEPER_SERVERS\"" \
| sudo tee /etc/default/docker

# restart the service to apply new options:
sudo service docker restart

echo "let's wait a little..."
sleep 30

# make this machine join the Docker Swarm cluster:
docker run -d --restart=always swarm join --advertise=$PRIVATE_IP:2375 zk://$ZOOKEEPER_SERVERS
```
Create a smaller maschine from the same image to later host the swarm manager it needs the same ports and further more port 8080 and 22 open to the worlds and a public ip address (to connect via http and ssh).

### ZooKeeper
Choose 3 machines to host the ZooKeeper Nodes and make sure zk1.os.baqend.com points to the first, zk2.os.baqend.com to the second and zk3.os.baqend.com to the third (internal ip addresses).

Start the ZooKeepers:
```
docker -H tcp://zk1.cloud:2375 run -d --restart=always \
      -p 2181:2181 \
      -p 2888:2888 \
      -p 3888:3888 \
      -v /var/lib/zookeeper:/var/lib/zookeeper \
      -v /var/log/zookeeper:/var/log/zookeeper  \
      --name zk1 \
      baqend/zookeeper zk1.os.baqend.com,zk2.os.baqend.com,zk3.os.baqend.com 1
docker -H tcp://zk2.os.baqend.com:2375 run -d --restart=always \
      -p 2181:2181 \
      -p 2888:2888 \
      -p 3888:3888 \
      -v /var/lib/zookeeper:/var/lib/zookeeper \
      -v /var/log/zookeeper:/var/log/zookeeper  \
      --name zk2 \
      baqend/zookeeper zk1.os.baqend.com,zk2.os.baqend.com,zk3.os.baqend.com 2
docker -H tcp://zk3.os.baqend.com:2375 run -d --restart=always \
      -p 2181:2181 \
      -p 2888:2888 \
      -p 3888:3888 \
      -v /var/lib/zookeeper:/var/lib/zookeeper \
      -v /var/log/zookeeper:/var/log/zookeeper  \
      --name zk3 \
      baqend/zookeeper zk1.os.baqend.com,zk2.os.baqend.com,zk3.os.baqend.com 3
```

### Swarm Manager

docker run -d --restart=always \
      --label role=manager \
      -p 2376:2375 \
      swarm manage zk://zk1.os.baqend.com,zk2.os.baqend.com,zk3.os.baqend.com
			
### Overlay Network
Talk to the swarm manager:

`export DOCKER_HOST=tcp://localhost:2376`

Create a overlay network for the containers to communicate:

`docker network create --driver overlay --subnet 10.0.0.0/16 bqnet`

## Setup Mongo

### Config Servers
Start 3 mongo config servers:

`docker run -d --name mongo-config1 --restart=always --net=bqnet -e constraint:az==server0 mongo --configsvr --replSet configReplSet --storageEngine wiredTiger`

`docker run -d --name mongo-config2 --restart=always --net=bqnet -e constraint:az==server2 mongo --configsvr --replSet configReplSet --storageEngine wiredTiger`

`docker run -d --name mongo-config3 --restart=always --net=bqnet -e constraint:az==server3 mongo --configsvr --replSet configReplSet --storageEngine wiredTiger`

Connect to one of the started config servers:

`docker exec -it mongo-config1 mongo --port 27019`

Bundle them into a replica set:

```
rs.initiate( {
   _id: "configReplSet",
   configsvr: true,
   members: [
      { _id: 0, host: "mongo-config1:27019" },
      { _id: 1, host: "mongo-config2:27019" },
      { _id: 2, host: "mongo-config3:27019" }
   ]
} )
```

### Sharding
Start 2 stand-alone shards on server 2:

`docker run -d --name mongo1 --restart=always --net=bqnet -e constraint:az==server2 mongo --storageEngine wiredTiger`

`docker run -d --name mongo2 --restart=always --net=bqnet -e constraint:az==server2 mongo --storageEngine wiredTiger`

### MongoS
Start 3 mongos, one for each REST server:

`docker run -d --name mongos1 --restart=always --net=bqnet -e constraint:az==server3 mongo mongos --configdb configReplSet/mongo-config1:27019,mongo-config2:27019,mongo-config3:27019`

`docker run -d --name mongos2 --restart=always --net=bqnet -e constraint:az==server4 mongo mongos --configdb configReplSet/mongo-config1:27019,mongo-config2:27019,mongo-config3:27019`

`docker run -d --name mongos3 --restart=always --net=bqnet -e constraint:az==server5 mongo mongos --configdb configReplSet/mongo-config1:27019,mongo-config2:27019,mongo-config3:27019`

### Register shards

Register the shards using one of the mongos:

`docker exec -it mongo mongo`

Register replicaSet shards with:

`sh.addShard( "rs[1..n]/mongo1-rs[1..n]:27017" )`

Register stand-alone shards with:
 
`sh.addShard( "mongo[1..n]:27017" )`

### Enable Sharding
Enable sharding for the `test`-database:

`sh.enableSharding("test")`
	
Shard the `test.bucket.Value`-collection:

`sh.shardCollection("test.test.bucket.Value", {_id: 1})`

Define the split-point for the shards:

`sh.splitAt("test.test.bucket.Value", {_id: "/db/test.bucket.Value/5000"})`
	
## Setup Redis
`docker run -d --name redis --restart=always -e constraint:az==server3 redis redis-server --appendonly yes`

## Setup Orestes and Node Servers

### Preperations

Make sure the orestes and node docker project is pulled onto the servers 3,4 and 5:

`bla bla bla`

Each server needs its own config file for the REST server to connect to its respective mongos.

1. Connect to the server:

`docker run -v /etc/orestes:/orestes -it -e constraint:az==server3 --rm busybox`

2. Alter the config:

`vi orestes/config.json`

3. Paste the following config:

```
{
  "gzip": false,
  "http": {
    "port": 8080,
    "idleTimeout": 0
  },
  "caching": {
    "clientCaching": false
  },
  "bloomfilter": {
    "enabled": false
  },
  "state" : {
    "location" : "redis://redis:6379"
  },
  "node": {
    "port": "8081",
    "runCommand": null
  },
  "mongo": {
    "enabled": true,
    "servers": ["tcp://mongos1:27017"]
  },
  "locking": {
    "enabled": true,
    "location" : "tcp://redis:6379"
  }
}
```

4. Repeat for all REST servers.

### Starting Server with Docker-Compose
Create a `docker-compose.yml` containing:

```
version: '2'
services:
  orestes:
    image: docker.baqend.com/baqend/orestes:1.2.0
    restart: always
    networks: 
      bqnet:
      default:
        aliases:
          - orestes
    environment: 
      - constraint:az==server3
    command: --config /etc/orestes/config.json --app test
    volumes:
      - /etc/orestes/config.json:/etc/orestes/config.json
  node:
    image: docker.baqend.com/baqend/node:1.2.0
    restart: always
    networks: 
      - default
    links:
      - "orestes:orestes"
  tacli:
    image: erikwitt/transaction-performance:latest
    networks:
      - default
    environment:
      - constraint:az==server1
    command: -h orestes -r 100 -t
networks:
  bqnet:
    external: true
```

Start the server:

`docker-compose -p rest_1 up -d`

Alter the server number in the `docker-compose.yml` and repeat the server start:

`docker-compose -p rest_2 up -d`

Again...

`docker-compose -p rest_3 up -d`

Initialize the database:

`docker-compose -p rest_1 scale tacli=1`

## Scale Clients
Alter the `tacli`-entry in `docker-compose.yml` to execute transaction instead of initalizing the database:

```
version: '2'
services:
  orestes:
    image: docker.baqend.com/baqend/orestes:1.2.0
    restart: always
    networks: 
      bqnet:
      default:
        aliases:
          - orestes
    environment: 
      - constraint:az==server3
    command: --config /etc/orestes/config.json --app test
    volumes:
      - /etc/orestes/config.json:/etc/orestes/config.json
  node:
    image: docker.baqend.com/baqend/node:1.2.0
    restart: always
    networks: 
      - default
    links:
      - "orestes:orestes"
  tacli:
    image: erikwitt/transaction-performance:latest
    networks:
      - default
    environment:
      - constraint:az==server1
    command: -h orestes -r 100 -t
networks:
  bqnet:
    external: true
```

Scale the number of clients for each server using docker-compose.

`docker-compose -p rest_1 scale tacli=5 & docker-compose -p rest_2 scale tacli=5 & docker-compose -p rest_3 scale tacli=5`

## Utilities for Debugging
Mount file system for docker logs:

`docker run -v /var/log/upstart:/upstart -it -e constraint:az==server2 --rm busybox`

Mount file system for docker service config

`docker run -v /etc/default/docker:/docker -it -e constraint:az==server5 --rm busybox`

