package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.PersonPhone

class PersonBuilder(
  var firstName: String = "NEO",
  var lastName: String = "AYOMIDE",
  var phoneNumbers: List<Pair<String, String>> = listOf(),
  var addressBuilders: List<PersonAddressBuilder> = listOf(),
) {
  fun build(): Person =
    Person(
      firstName = firstName,
      lastName = lastName,
    ).apply {
      phoneNumbers.forEach { (type, number) ->
        this.phones.add(
          PersonPhone(
            person = this,
            phoneType = type,
            phoneNo = number
          )
        )
      }
    }.apply {
      addressBuilders.forEach {
        this.addresses.add(it.build(this))
      }
    }
}
