#! /bin/bash -ex

VERSION=$(lein v show | grep -oE "[0-9]+\.[0-9]+\.[0-9]+[^,]*" | head -n 1)
echo "Going to build docker image for fischer version $VERSION..."

IMAGE_NAME="chillkroetetts/fischer:$VERSION"

docker build -t $IMAGE_NAME .

docker login

docker push $IMAGE_NAME
