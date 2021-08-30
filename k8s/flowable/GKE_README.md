# Flowable on Google Kubernetes Engine (GKE)

*see the README.md for all Flowable Helm chart configuration options*

*if you have a GKE clust and configured Kubernetes context active skip to part `Deploy Nginx Ingress`*

## Install prerequisites
### Install Brew 

To install the Brew package manager

```console
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install.sh)"
```

### Install Kubernetes CLI

```console
brew install kubectl
```

### Install Helm

```console
brew install kubectl
```

### Install and configure Google Cloud SDK

```console
brew cask install google-cloud-sdk

gcloud init
```

## Configure cluster

### Create Google Cloud Cluster

```console
gcloud container clusters create flowable-cluster \
    --zone europe-west1-b \
    --machine-type=n1-standard-2  --enable-autorepair \
    --enable-autoscaling --max-nodes=10 --min-nodes=1
```

### Deploy Nginx Ingress

deploys Nginx Ingress controller in the `nginx` namespace

```console
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

helm repo add stable https://kubernetes-charts.storage.googleapis.com

helm repo update

kubectl create ns nginx

helm install nginx ingress-nginx/ingress-nginx \
    --namespace nginx \
    --set rbac.create=true \
    --set controller.publishService.enabled=true
```

#### Determine publicIP

```console
kubectl --namespace nginx get services -o wide -w nginx-ingress-nginx-controller
```

use the `external-ip` value in the next step(s) 

## Deploy Flowable 

Deploys Flowabe Helm chart with Flowable REST active

```console
helm install flowable-6.7.0 flowable/ --set host.external=<external-ip> --set ui.enabled=false --set rest.enabled=true
```

### Check pod(s) status

```console
kubectl get pods -n default
```

When all pods are active and ready the application will be available on; 

http://\<external-ip\>/flowable-rest/

The Swagger docs UI app will be available on;

http://\<external-ip\>/flowable-rest/docs/

### Default credentials

The default credentials are;

username: rest-admin

password: admin