# HMPPS NOMIS Syncronisation Prisoner API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-nomis-priosner-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-nomis-prisoner-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://nomis-prsner-dev.aks-dev-1.studio-hosting.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs)

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
