#!/bin/bash
set -eou pipefail

readonly IMAGE=${1:-"flowable/flowable-jre:21"}
echo "Image name: ${IMAGE}"

echo "Building image..."
docker buildx create --name container --driver=docker-container
docker buildx build --tag ${IMAGE} --platform linux/amd64,linux/arm64 --builder container --push .
