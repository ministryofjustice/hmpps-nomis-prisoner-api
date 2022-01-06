package uk.gov.justice.digital.hmpps.hmppsnomisprisonerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsNomisPrisonerApi

fun main(args: Array<String>) {
  runApplication<HmppsNomisPrisonerApi>(*args)
}
