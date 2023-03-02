# Flowable Docker

## Run configurations

A few pre configured `Flowable REST` run configurations are provided.
These examples use `Docker Compose`.

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

To verify the Flowable REST 6.7.3 image execute the following command

```bash
cosign verify -key cosign.pub flowable/flowable-rest:6.7.3
```