#!/bin/bash
set -eou pipefail

readonly IMAGE=${1:-"flowable/flowable-jre:11.0.15"}
echo "Image name: ${IMAGE}"

echo "Building image..."
docker build -t ${IMAGE} -f Dockerfile .
