# Flowable

This is a helm chart for the [Flowable UI apps][flowable].

## TL;DR;

```console
helm install flowable
```

## Installing the Chart

To install the chart with the release name `my-flowable`:

```console
helm install --name my-flowable flowable
```

## Uninstalling the Chart

To uninstall/delete the `my-flowable` deployment:

```console
helm delete my-flowable --purge
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
| `cloudSql.credentials`                        | Google Cloud SQLcredentials secret reference                                                                          | `cloudsql-credentials.json`   |
| `ingress.enabled`                             | Enables Ingres                                                                                                        | `true`                        |
| `ingress.sslRedirect`                         | Enables SSL redirect                                                                                                  | `false`                       |
|<br/>|
| `idm.enabled`                                 | Enables Flowable IDM                                                                                                  | `true`                        |
| `idm.replicas`                                | Number of replicated pods                                                                                             | `1`                           |
| `idm.service.name`                            | Kubernetes service name                                                                                               | `flowable-idm`                |
| `idm.contextUrl`                              | Tomcat servlet mapping                                                                                                | `/`                           |
| `idm.ingressPath`                             | Ingress path mapping                                                                                                  | `flowable-idm`                |
| `idm.image.repository`                        | Docker image name                                                                                                     | `flowable/flowable-idm`       |
| `idm.image.tag`                               | Docker tag name                                                                                                       | `latest`                      |
| `idm.image.pullPolicy`                        | Docker pull policy                                                                                                    | `Always`                      |
| `idm.resources.requests.cpu`                  | Kubernetes CPU request                                                                                                | `100m`                        |
| `idm.resources.requests.memory`               | Kubernetes memory request                                                                                             | `1Gi`                         |
| `idm.resources.limits.cpu`                    | Kubernetes CPU limit                                                                                                  | `1`                           |
| `idm.resources.limits.memory`                 | Kubernetes memory limit                                                                                               | `1Gi`                         |
| `idm.resources.javaOpts`                      | JVM options                                                                                                           | `-Xmx1g -Xms1g`               |
|<br/>|
| `task.enabled`                                | Enables Flowable Task (either enable Flowable Task or Flowable REST)                                                  | `true`                        |
| `task.replicas`                               | Number of replicated pods                                                                                             | `1`                           |
| `task.service.name`                           | Kubernetes service name                                                                                               | `flowable-task`               |
| `task.contextUrl`                             | Tomcat servlet mapping                                                                                                | `/`                           |
| `task.ingressPath`                            | Ingress path mapping                                                                                                  | `flowable-task`               |
| `task.image.repository`                       | Docker image name                                                                                                     | `flowable/flowable-task`      |
| `task.image.tag`                              | Docker tag name                                                                                                       | `latest`                      |
| `task.image.pullPolicy`                       | Docker pull policy                                                                                                    | `Always`                      |
| `task.resources.requests.cpu`                 | Kubernetes CPU request                                                                                                | `100m`                        |
| `task.resources.requests.memory`              | Kubernetes memory request                                                                                             | `1Gi`                         |
| `task.resources.limits.cpu`                   | Kubernetes CPU limit                                                                                                  | `1`                           |
| `task.resources.limits.memory`                | Kubernetes memory limit                                                                                               | `1Gi`                         |
| `task.resources.javaOpts`                     | JVM options                                                                                                           | `-Xmx1g -Xms1g`               |
|<br/>|
| `rest.enabled`                                | Enables Flowable REST (either enable Flowable Task or Flowable REST)                                                  | `false`                       |
| `rest.replicas`                               | Number of replicated pods                                                                                             | `1`                           |
| `rest.service.name`                           | Kubernetes service name                                                                                               | `flowable-rest`               |
| `rest.contextUrl`                             | Tomcat servlet mapping                                                                                                | `/`                           |
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
| `modeler.enabled`                             | Enables Flowable REST (either enable Flowable Task or Flowable REST)                                                  | `true`                        |
| `modeler.replicas`                            | Number of replicated pods                                                                                             | `1`                           |
| `modeler.service.name`                        | Kubernetes service name                                                                                               | `flowable-modeler`            |
| `modeler.contextUrl`                          | Tomcat servlet mapping                                                                                                | `/`                           |
| `modeler.ingressPath`                         | Ingress path mapping                                                                                                  | `flowable-modeler`            |
| `modeler.image.repository`                    | Docker image name                                                                                                     | `flowable/flowable-modeler`   |
| `modeler.image.tag`                           | Docker tag name                                                                                                       | `latest`                      |
| `modeler.image.pullPolicy`                    | Docker pull policy                                                                                                    | `Always`                      |
| `modeler.resources.requests.cpu`              | Kubernetes CPU request                                                                                                | `100m`                        |
| `modeler.resources.requests.memory`           | Kubernetes memory request                                                                                             | `1Gi`                         |
| `modeler.resources.limits.cpu`                | Kubernetes CPU limit                                                                                                  | `1`                           |
| `modeler.resources.limits.memory`             | Kubernetes memory limit                                                                                               | `1Gi`                         |
| `modeler.resources.javaOpts`                  | JVM options                                                                                                           | `-Xmx1g -Xms1g`               |
|<br/>|
| `admin.enabled`                               | Enables Flowable Admin                                                                                                | `true`                        |
| `admin.replicas`                              | Number of replicated pods                                                                                             | `1`                           |
| `admin.service.name`                          | Kubernetes service name                                                                                               | `flowable-admin`              |
| `admin.contextUrl`                            | Tomcat servlet mapping                                                                                                | `/`                           |
| `admin.ingressPath`                           | Ingress path mapping                                                                                                  | `flowable-admin`              |
| `admin.image.repository`                      | Docker image name                                                                                                     | `flowable/flowable-admin`     |
| `admin.image.tag`                             | Docker tag name                                                                                                       | `latest`                      |
| `admin.image.pullPolicy`                      | Docker pull policy                                                                                                    | `Always`                      |
| `admin.resources.requests.cpu`                | Kubernetes CPU request                                                                                                | `100m`                        |
| `admin.resources.requests.memory`             | Kubernetes memory request                                                                                             | `1Gi`                         |
| `admin.resources.limits.cpu`                  | Kubernetes CPU limit                                                                                                  | `1`                           |
| `admin.resources.limits.memory`               | Kubernetes memory limit                                                                                               | `1Gi`                         |
| `admin.resources.javaOpts`                    | JVM options                                                                                                           | `-Xmx1g -Xms1g`               |
|<br/>|
| `nginx-ingress.enabled`                       | Deploys [NGINX Ingress controller][nginx-ingress]                                                                     | `false`                       |
| `nginx-ingress.rbac.create`                   | Creates Ingress controller with RBAC                                                                                  | `true`                        |
| `nginx-ingress.rbac.create`                   | When enabled the controller will set the endpoint records on the ingress objects to reflect those on the service      | `true`                        |
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
helm install --name my-flowable \
  --set admin.enabled=false \
    flowable
```

Alternatively, a YAML file that specifies the values for the above parameters can be provided while installing the chart. For example,

```console
helm install --name my-flowable -f values.yaml flowable
```

[flowable]: https://github.com/flowable/flowable-engine
[nginx-ingress]: https://github.com/kubernetes/ingress-nginx