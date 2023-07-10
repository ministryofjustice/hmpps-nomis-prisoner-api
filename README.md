# HMPPS NOMIS Synchronisation Prisoner API

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-nomis-prisoner-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-nomis-prisoner-api)
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

# Nomis Data DSL
TODO 
* working OK now
* create the markdown from the leaf and remove markdown stuff from main function
* then can also create html from the leaf
* then push the html out to a file???
* then the gradle task
* and then can we work out the default values by calling the function? https://discuss.kotlinlang.org/t/retrieve-default-parameter-value-via-reflection/7314/5
* do we even need javapoet?
* ...or why not use it to read the source files, then we get the default values easier

## datq

