# About

To test the RedirectPort-Registry service during a certain amount of time.

# Features

* Can test with the bridge encrypted or not
* Can choose the delay between messages to send
* Can choose the message size 

# Usage

```bash
# Compile
./gradlew clean build

# Unzip
cd build/distributions/
tar -xf redirectport-registry-test-master-SNAPSHOT.tar
cd ../../

# Start a container
REDIRECT_DOCKER_IMAGE=foilen/redirectport-registry:latest
CONTAINER_NAME=redirect-test
docker run -ti \
  --rm \
  --detach \
  --name $CONTAINER_NAME \
  $REDIRECT_DOCKER_IMAGE \
  /bin/bash

# Copy this test app to the container
docker cp build/distributions/redirectport-registry-test-master-SNAPSHOT $CONTAINER_NAME:/app-test/

# Go inside
docker attach $CONTAINER_NAME

# From inside
/app-test/bin/redirectport-registry-test \
  --encrypted \
  --delaySec 1 \
  --payloadSizeBytes 1024

# Ctrl+c when you want to stop

# Stop everything
exit
```
