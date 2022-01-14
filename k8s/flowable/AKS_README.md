# Flowable Open Soruce on Azure Kubernetes Service (AKS)

*see the README.md for all Flowable Helm chart configuration options*

*if you have a AKS cluster and configured Kubernetes context active skip to part `Deploy Nginx Ingress`*

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
brew install helm
```

### Install and configure Azure CLI

```console
brew install azure-cli

az login
```

## Configure cluster

### Create AKS cluster

```console
az group create --name flowable-oss-rg --location westeurope
```

>You can choose a different location. A list of available locations for your account can be fetched with the command below. Mind: when changed use this other location consistently when referred to in other commands.

```console
az account list-locations -o table
```

```console
az aks create --resource-group flowable-oss-rg --name flowable-oss \
--node-count 2 --enable-addons monitoring --generate-ssh-keys
```

### Configure kubectl context

```console
az aks get-credentials --resource-group flowable-oss-rg --name flowable-oss
```

### Deploy Nginx Ingress

deploys Nginx Ingress controller in the `nginx` namespace

```console
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx

helm repo update

helm install nginx ingress-nginx/ingress-nginx \
    --namespace ingress-nginx \
    --create-namespace \
    --set rbac.create=true \
    --set controller.publishService.enabled=true \
    --set controller.service.annotations."service\.beta\.kubernetes\.io/azure-dns-label-name"=<dns label>
```

*Replace `<dns label>` with something of your own liking. The cluster (and Flowable application) will be reachable on https://`<dns label>`.westeurope.cloudapp.azure.com/flowable-ui*

#### Check the deployment status; LoadBalancer IP availability 

```console
kubectl --namespace ingress-nginx get services -o wide -w nginx-ingress-nginx-controller
```

### Deploy Cert Manager

``` console
helm repo add jetstack https://charts.jetstack.io

helm repo update

helm install cert-manager jetstack/cert-manager \
--namespace cert-manager --create-namespace \
 --version v1.6.1 --set installCRDs=true
```

### Create Cluster Issuer

```console
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: <your email>
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
      - http01:
          ingress:
            class: nginx
EOF
```

*replace `<your email>` with your email*

## Deploy Flowable OSS

Deploys Flowabe Helm chart with Flowable REST active

```console
helm repo add flowable-oss https://flowable.github.io/helm/

helm repo update

helm install my-flowable flowable-oss/flowable --version 6.7.3-snapshot.2 --devel \
--namespace flowable --create-namespace \
--set host.external=<dns-label>westeurope.cloudapp.azure.com --set ingress.useHost=true \
--set ingress.clusterIssuer=letsencrypt-prod \
--set postgres.storage.storageClassName=default

```

### Check pod(s) status

```console
kubectl get pods -n flowable -w
```

When all pods are active and ready the application will be available on; 

https://\<dns-label\>.westeurope.cloudapp.azure.com/flowable-ui

### Default credentials

The default credentials are;

username: admin

password: test