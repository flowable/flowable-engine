# Flowable Kubernetes

Flowable HELM chart is located in the *flowable* folder. 
The Kubernetes configuration files are located in the *resources* folder.

## Install HELM

### Install HELM client

```brew install kubernetes-helm```

(or follow instructions here https://helm.sh/docs/using_helm/#installing-helm)

### Install HELM server

```helm init```

## Requirements

### Install Kubernetes Ingress Nginx

*for Docker Desktop*

```kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/mandatory.yaml```

```kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/provider/cloud-generic.yaml```

(or follow instructions here https://github.com/kubernetes/ingress-nginx/blob/master/docs/deploy/index.md)

### Install local storageclass

```kubectl apply -f resources/local-storageclass.yaml```

## Deploy Flowable OSS

```helm install flowable --name=flowable```

## Undeploy Flowalbe OSS

```helm delete flowable --purge```