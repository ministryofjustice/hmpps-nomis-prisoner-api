package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonInternetAddress

@DslMarker
annotation class PersonEmailDslMarker

@NomisDataDslMarker
interface PersonEmailDsl

@Component
class PersonEmailBuilderFactory {
  fun builder() = PersonEmailBuilder()
}

class PersonEmailBuilder : PersonEmailDsl {

  fun build(
    person: Person,
    emailAddress: String,
  ): PersonInternetAddress =
    PersonInternetAddress(
      person = person,
      emailAddress = emailAddress,
    )
}
