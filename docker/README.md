# Flowable Docker

## Run configurations

A few pre configured `Flowable UI` and `Flowable REST` run configurations are provided.
These examples use `Docker Compose`.

### Flowable UI with PostgreSQL 

```bash
./ui-postgres.sh start|stop|info
```

Available on: http://localhost:8080/flowable-ui/

login/password: `admin/test`

### Flowable UI with Keycloak and PostgreSQL 

```bash
./ui-keycloak-postgres.sh start|stop|info
```

*--additional configuration required--*

In order to be able to redirect both Docker containers and client browser to the same authentication provider (Keycloak) uri `keycloak.flowable.org` must be added as an alias to the Docker host hostname.

For local development this can be done by modifying the `/etc/hosts`;

```
127.0.0.1	localhost keycloak.flowable.org
```

Available on: http://localhost:8080/flowable-ui/

login/password 

admin: `admin@flowable/test`

modeler: `modeler@flowable/test`

workflow: `workflow@flowable/test`

Keycloak available on: http://keycloak.flowable.org:8088/auth/

login/password: admin/admin


### Flowable REST with PostgreSQL 

```bash
./rest-postgres.sh start|stop|info
```

Available on: http://localhost:8080/flowable-rest/

Swagger available on:  http://localhost:8080/flowable-rest/docs/

login/password: `rest-admin/test`

### Flowable REST with loadbalancer (HAProxy) and PostgreSQL 

```bash
./rest-loadbalancer-postgres.sh start|stop|scale|info
```

Available on: http://localhost:8080/flowable-rest/

Swagger available on:  http://localhost:8080/flowable-rest/docs/

login/password: `rest-admin/test`

## [WIP] Signed images

Starting with version 6.7.3 the Flowable Docker images are being signed with [cosign](https://github.com/sigstore/cosign).
The public key (cosign.pub) can be used for verification.

```
-----BEGIN PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEhP8cYZE0aRlNd6kjTQuFSLliSkEV
n+aoyzYbqZMCcC7r75+5IchTZNImrYRxSSwph+JeVgOQ3Tbci03F/MjrqQ==
-----END PUBLIC KEY-----
```

To verify the Flowable UI 6.7.3 image execute the following command

```bash
cosign verify -key cosign.pub flowable/flowable-ui:6.7.3
```