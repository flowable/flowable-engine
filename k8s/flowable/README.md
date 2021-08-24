# Flowable

This is a helm chart for the [Flowable UI apps][flowable].

## TL;DR;

```console
helm repo add flowable https://flowable.org/helm/

helm install my-flowable flowable/flowable
```

## Installing the Chart

To install the *local* chart with the release name `my-flowable`:

```console
helm install my-flowable ./flowable
```

To install the *repo* chart with the release name `my-flowable`:

```console
helm repo add flowable https://flowable.org/helm/

helm install my-flowable ./flowable \
    --create-namespace --namespace=flowable \
    --set host.external=<cluster public ip / hostname> --set ingress.useHost=true \
    --set postgres.storage.storageClassName=default
```

This will install Flowable as the *my-flowable* release in the *flowable* namespace.
It will also configure Ingress mapping rules for route on the specified *host*.
The *StorageClassName* will be set to *default*.

## Uninstalling the Chart

To uninstall/delete the `my-flowable` deployment:

```console
helm remove my-flowable
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Configuration

The following tables lists the configurable parameters of the Unifi chart and their default values.

| Parameter                                     | Description                                                                                                           | Default                       |
| --------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- | ----------------------------- |
| `host.external`                               | External cluster IP or FQDN                                                                                           | `localhost`                   |
| `database.name`                               | Database name                                                                                                         | `flowable`                    |
| `database.username`                           | Database user                                                                                                         | `flowable`                    |
| `database.password`                           | Database password                                                                                                     | `flowable`                    |
| `cloudSql.enabled`                            | Enables Google Cloud SQL for database                                                                                 | `false`                       |
| `cloudSql.credentials`                        | Google Cloud SQL credentials secret reference                                                                         | `cloudsql-credentials.json`   |
| `ingress.enabled`                             | Enables Ingres                                                                                                        | `true`                        |
| `ingress.sslRedirect`                         | Enables SSL redirect                                                                                                  | `false`                       |
| `ingress.useHost`                             | Enables host based routing using external `host.external` ( this must be a FQDN)                                           | `false`                            |
| `ingress.class`                               | Ingress class name                  | `nginx`
|<br/>|
| `ui.enabled`                                | Enables Flowable UI (either enable Flowable UI or Flowable REST)                                                  | `false`                        |
| `ui.replicas`                               | Number of replicated pods                                                                                             | `1`                           |
| `ui.service.name`                           | Kubernetes service name                                                                                               | `flowable-ui`               |
| `ui.contextPath`                             | Tomcat servlet mapping                                                                                                | `/`                           |
| `ui.ingressPath`                            | Ingress path mapping                                                                                                  | `flowable-ui`               |
| `ui.image.repository`                       | Docker image name                                                                                                     | `flowable/flowable-ui`      |
| `ui.image.tag`                              | Docker tag name                                                                                                       | `latest`                      |
| `ui.image.pullPolicy`                       | Docker pull policy                                                                                                    | `Always`                      |
| `ui.resources.requests.cpu`                 | Kubernetes CPU request                                                                                                | `100m`                        |
| `ui.resources.requests.memory`              | Kubernetes memory request                                                                                             | `1Gi`                         |
| `ui.resources.limits.cpu`                   | Kubernetes CPU limit                                                                                                  | `1`                           |
| `ui.resources.limits.memory`                | Kubernetes memory limit                                                                                               | `1Gi`                         |
| `ui.resources.javaOpts`                     | JVM options                                                                                                           | `-Xmx1g -Xms1g`               |
|<br/>|
| `rest.enabled`                                | Enables Flowable REST (either enable Flowable UI or Flowable REST)                                                  | `true`                       |
| `rest.replicas`                               | Number of replicated pods                                                                                             | `1`                           |
| `rest.service.name`                           | Kubernetes service name                                                                                               | `flowable-rest`               |
| `rest.contextPath`                             | Tomcat servlet mapping                                                                                                | `/`                           |
| `rest.ingressPath`                            | Ingress path mapping                                                                                                  | `flowable-rest`               |
| `rest.image.repository`                       | Docker image name                                                                                                     | `flowable/flowable-rest`      |
| `rest.image.tag`                              | Docker tag name                                                                                                       | `latest`                      |
| `rest.image.pullPolicy`                       | Docker pull policy                                                                                                    | `Always`                      |
| `rest.resources.requests.cpu`                 | Kubernetes CPU request                                                                                                | `100m`                        |
| `rest.resources.requests.memory`              | Kubernetes memory request                                                                                             | `1Gi`                         |
| `rest.resources.limits.cpu`                   | Kubernetes CPU limit                                                                                                  | `1`                           |
| `rest.resources.limits.memory`                | Kubernetes memory limit                                                                                               | `1Gi`                         |
| `rest.resources.javaOpts`                     | JVM options                                                                                                           | `-Xmx1g -Xms1g`               |
|<br/>|
| `postgres.enabled`                            | Will deploy and configure a PostgreSQL database instance                                                              | `true`                        |
| `postgres.service.name`                       | Kubernetes service name                                                                                               | `flowable-postgres`           |
| `postgres.storage.storageClassName`           | Kubernetes storage class name for peristent volume claim                                                              | `standard`                    |
| `postgres.storage.size`                       | Peristent volume claim resource request size                                                                          | `1Gi`                         |
| `postgres.resources.requests.cpu`             | Kubernetes CPU request                                                                                                | `100m`                        |
| `postgres.resources.requests.memory`          | Kubernetes memory request                                                                                             | `1Gi`                         |
| `postgres.resources.limits.cpu`               | Kubernetes CPU limit                                                                                                  | `1000m`                       |
| `postgres.resources.limits.memory`            | Kubernetes memory limit                                                                                               | `1Gi`                         |

Specify each parameter using the `--set key=value[,key=value]` argument to `helm install`. For example,

```console
helm install my-flowable \
  --set admin.enabled=false \
    flowable
```

Alternatively, a YAML file that specifies the values for the above parameters can be provided while installing the chart. For example,

```console
helm install my-flowable -f values.yaml flowable
```


[flowable]: https://github.com/flowable/flowable-engine