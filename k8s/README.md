# Flowable Kubernetes

Flowable HELM chart is located in the *flowable* folder. 
The Kubernetes configuration files are located in the *resources* folder.

## Install Helm

### Install Helm client

```console
brew install kubernetes-helm
```

(or follow instructions here https://helm.sh/docs/using_helm/#installing-helm)

### Install Helm server (Tiller)

```console
helm init
```

## Requirements

### Install Kubernetes Ingress Nginx

*for Docker Desktop*

```console
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/mandatory.yaml
```

```console
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/master/deploy/static/provider/cloud-generic.yaml
```

(or follow instructions here https://github.com/kubernetes/ingress-nginx/blob/master/docs/deploy/index.md)

### Install local storageclass

```console
kubectl apply -f resources/local-storageclass.yaml
```

## Deploy Flowable OSS

```console
helm install flowable \
    --name=flowable \
    --set flowable.host.external=<cluster public ip / hostname>
```

***flowable.host.external** will be used for client redirects*  

## Undeploy Flowable OSS

```console
helm delete flowable --purge
```