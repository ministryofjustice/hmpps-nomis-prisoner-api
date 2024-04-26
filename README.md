# HMPPS NOMIS Synchronisation Prisoner API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-nomis-prisoner-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-nomis-prisoner-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://nomis-prisoner-api-dev.prison.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)

This is a private API for synchronising data between NOMIS and DPS services that hold data outside of NOMIS.
It is part of a suite of services to support migration away from NOMIS and should only be used for synchronisation by the following services:
* hmpps-prisoner-from-nomis-migration   
* hmpps-prisoner-to-nomis-update


## Building

```./gradlew build```

## Running

Various methods to run the application locally are detailed below.

Once up the application should be available on port 8101 - see the health page at http://localhost:8101/health.

Also try http://localhost:8101/swagger-ui/configUrl=/v3/api-docs to see the API specification.


### Running the application against dev (t3) database
Steps are:
1. Create a tunnel to the database
```shell
ssh -f -D 1086 -N -Snone hmppgw1 -L1521:t3nomis-b.test.nomis.service.justice.gov.uk:1521
```
For the tunnel to work you will need host information for `hmppgw1` in your `~/.ssh/config`.  Please see
[confluence](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/800686284/Accessing+and+Developing+in+the+T3+Environment#SSH-config)
for more information.

2. Start Nomis Prisoner API with the following VM options
```shell
-Doracle.jdbc.J2EE13Compliant=true -Xmx1024m -Xms1024m
```
and with profiles
```shell
oracle,stdout
```
and the following environment variables
```shell
API_BASE_URL_OAUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
DB_USERNAME=<your t3 user> 
DB_PASSWORD=<your t3 password>
JDBC_URL=jdbc:oracle:thin:@//localhost:1521/NOMIS_TAF
```
