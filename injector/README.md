# PubSub Taxi Rides feeder program written in Go

## Google Compute Engine VM creation
* Login into Google Cloud Console and select Google Compute Engine from the menu
* Click “CREATE INSTANCE”. 
* Provide name of the vm say “tr-feeder”
* Select any zone say “us-west1-b”
* In “Machine Type”, select customize and specify 8 core and 50 GB RAM
* In “Boot disk” click on change 
* Select “Ubuntu 16.04 LTS” in OS images
* Increase the Hard disk size to 20 GB and click “Select”
* Click “Select” button to create VM


## Setup environment in Google compute engine instance
* Login into Google Cloud Console and select Google Compute Engine from the menu
* Click on the “SSH” dropdown adjacent to created VM and click “Open in browser window”
* In terminal switch to root user using _sudo su -_
* Install golang (https://golang.org/doc/install) using below commands
* _mkdir Downloads_
* _cd Downloads_
* _wget https://storage.googleapis.com/golang/go1.8.linux-amd64.tar.gz tar -C /usr/local -xzf go1.8.linux-amd64.tar.gz_
* _mkdir -p /root/work_
* vi ~/.bashrc and add the below content at bottom
``export PATH=$PATH:/usr/local/go/bin
export GOPATH=/root/work``
* Save ~/.bashrc and exit from vi
* Execute _source ~/.bashrc_
* Install git using _sudo apt-get install -y git_ (if not already installed)
* Clone current repository using below commands
* _cd /root_
* _git clone https://github.com/springml/nycity_taxi_ad_api.git_
* _cd nycity_taxi_ad_api/injector_
* Install necessary go packages using below commands
```go get cloud.google.com/go/bigquery
go get google.golang.org/genproto/googleapis/pubsub/v1
go get googlemaps.github.io/maps
go get github.com/namsral/flag
```
* Edit __config_without_docker.sh__ for any injector parameter changes
* Export the injector parameters using - _source config_without_docker.sh_
* Run injector using _go run *.go_

