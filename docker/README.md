# Flowable Docker run configurations

A few pre configured `Flowable UI` and `Flowable REST` run configurations are provided.
These examples use `Docker Compose`.

## Flowable UI with PostgreSQL 

```bash
./ui-postgres.sh start|stop|info
```

Available on: http://localhost:8080/flowable-ui/

login/password: `admin/test`

## Flowable UI with Keycloak and PostgreSQL 

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


## Flowable REST with PostgreSQL 

```bash
./rest-postgres.sh start|stop|info
```

Available on: http://localhost:8080/flowable-rest/

Swagger available on:  http://localhost:8080/flowable-rest/docs/

login/password: `rest-admin/test`

## Flowable REST with loadbalancer (HAProxy) and PostgreSQL 

```bash
./rest-loadbalancer-postgres.sh start|stop|scale|info
```

Available on: http://localhost:8080/flowable-rest/

Swagger available on:  http://localhost:8080/flowable-rest/docs/

login/password: `rest-admin/test`