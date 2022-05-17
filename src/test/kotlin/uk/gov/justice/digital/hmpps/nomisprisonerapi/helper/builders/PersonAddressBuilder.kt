package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.AddressPhone
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonAddress

class PersonAddressBuilder(
  var premise: String = "41",
  var street: String = "High Street",
  var locality: String = "Sheffield",
  private var phoneNumbers: List<Triple<String, String, String?>> = listOf(),
) {
  fun build(person: Person): PersonAddress =
    PersonAddress(
      person = person,
      premise = premise,
      street = street,
      locality = locality
    ).apply {
      phoneNumbers.map { (type, number, extension) ->
        this.addPhone(
          AddressPhone(
            this,
            phoneType = type,
            phoneNo = number,
            extNo = extension
          )
        )
      }
    }
}
