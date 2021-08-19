# Flowable Kubernetes

## Requirements

* Kubernetes v1.16+
* Helm 3

Flowable UI can be deployed to a Kubernetes cluster using the `flowable-ui.yaml` manifest located in the `resources` folder.
This manifest contains 'only' the deployment and service descriptors for Flowable UI. 

A more preferred way of deploying FlowableUI is using the Helm chart. This deploys Flowable using a predefined configuration; which can be overriden.
By default the chart will deploy a PostgreSQL instance. For this a *PersistantVolume* is required. Because different cloud providers have different implementations the applicable *storage class* must be provided when deploying in order to create the *PersistantVolumeClaim*.

There are several ways to expose the deployed Flowable UI service on the Kubernetes cluster to the outside world.
For convenience the Flowable Helm chart includes *ingress rules* that can be used to configure an *Ingress controller*. For this the *Ingress controller* must be present and configured on the cluster.
By default an Ingress with the annotation `kubernetes.io/ingress.class: "nginx"` will located. This class is configurable.

Info on how to install a Nginx ingress controller can be found here; 
[Ingress-Nginx](https://github.com/kubernetes/ingress-nginx/tree/main/charts/ingress-nginx).


## Deploy Flowable OSS

```console
helm repo add flowable https://flowable.org/helm/
```
```console
helm install flowable flowable/flowable \
    --create-namespace --namespace=flowable \
    --set host.external=<cluster public ip / hostname> --set ingress.useHost=true \
    --set postgres.storage.storageClassName=default
```

***flowable.host.external** will be used for client redirects*  

Check for individual pod status

```console
kubectl get pods -n flowable -w
```

## Undeploy Flowable OSS

```console
helm delete flowable -n flowable
```