package uk.gov.justice.digital.hmpps.nomisprisonerapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy

@SpringBootApplication
@EnableAspectJAutoProxy
class NomisPrisonerApi

fun main(args: Array<String>) {
  runApplication<NomisPrisonerApi>(*args)
}
