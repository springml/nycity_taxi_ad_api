#!/bin/bash
# source this confing file with
# . config_without_docker.bash
# For docker builds, environment variables can be provided
# in a properties.env file so this is not needed.
# export PROJECT=$(curl -H "Metadata-Flavor: Google" "http://metadata.google.internal/computeMetadata/v1/project/project-id")

export PROJECT=billion-taxi-rides
export BUCKET=billion-taxi-rides
export FILEPREFIX=json/yellow_tripdata_2015-01-ext
export PUBSUBTOPIC=smlfeed
export SPEEDUP=1
export PUBSUBBATCHSIZE=300
export ROUTEINFOFREQ=10
export SKIPRIDES=1
export SKIPFILES=1
export DEBUG=true
