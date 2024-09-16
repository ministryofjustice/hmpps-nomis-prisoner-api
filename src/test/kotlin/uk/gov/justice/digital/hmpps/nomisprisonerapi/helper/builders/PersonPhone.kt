package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone

@DslMarker
annotation class PersonPhoneDslMarker

@NomisDataDslMarker
interface PersonPhoneDsl

@Component
class PersonPhoneBuilderFactory {
  fun builder() = PersonPhoneBuilder()
}

class PersonPhoneBuilder : PersonPhoneDsl {

  fun build(
    person: Person,
    phoneType: String,
    phoneNo: String,
    extNo: String?,
  ): PersonPhone =
    PersonPhone(
      person = person,
      phoneType = phoneType,
      phoneNo = phoneNo,
      extNo = extNo,
    )
}
