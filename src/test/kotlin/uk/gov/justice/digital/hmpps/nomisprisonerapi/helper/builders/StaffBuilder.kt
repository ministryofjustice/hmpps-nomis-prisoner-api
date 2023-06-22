package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Staff

class StaffBuilder(
  private var firstName: String = "ANDREW",
  private var lastName: String = "BENNETT",
) : StaffDsl {
  fun build(): Staff =
    Staff(firstName = firstName, lastName = lastName)
}
