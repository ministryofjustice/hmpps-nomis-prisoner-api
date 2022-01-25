package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person

class PersonBuilder(
  var firstName: String = "NEO",
  var lastName: String = "AYOMIDE"
) {
  fun build(): Person =
    Person(
      firstName = firstName,
      lastName = lastName,
    )
}
