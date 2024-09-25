package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Corporate
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmployment
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonEmploymentPK

@DslMarker
annotation class PersonEmploymentDslMarker

@NomisDataDslMarker
interface PersonEmploymentDsl

@Component
class PersonEmploymentBuilderFactory {
  fun builder() = PersonEmploymentBuilder()
}

class PersonEmploymentBuilder : PersonEmploymentDsl {

  fun build(
    person: Person,
    sequence: Long,
    active: Boolean,
    employerCorporate: Corporate?,
  ): PersonEmployment =
    PersonEmployment(
      id = PersonEmploymentPK(person, sequence),
      active = active,
      employerCorporate = employerCorporate,
    )
}
