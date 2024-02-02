#!/bin/bash
set -eou pipefail

readonly IMAGE=${1:-"flowable/flowable-jre:17.0.9"}
echo "Image name: ${IMAGE}"

echo "Pushing image..."
docker image push ${IMAGE}
