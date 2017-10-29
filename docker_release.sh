#! /bin/bash -ex
: ${DOCKER_USER?Need a value}
: ${DOCKER_PW?Need a value}

VERSION=$(lein v show | grep -oE "[0-9]+\.[0-9]+\.[0-9]+[^,]*" | head -n 1)
echo "Going to build docker image for fischer version $VERSION..."

IMAGE_NAME="chillkroetetts/fischer:$VERSION"

docker build -t $IMAGE_NAME .

docker login -u $DOCKER_USER -p $DOCKER_PW

docker push $IMAGE_NAME
