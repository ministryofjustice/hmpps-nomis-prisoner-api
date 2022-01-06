package uk.gov.justice.digital.hmpps.nomisprisonerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NomisPrisonerApi

fun main(args: Array<String>) {
  runApplication<NomisPrisonerApi>(*args)
}
