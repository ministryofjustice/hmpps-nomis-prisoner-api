package uk.gov.justice.digital.hmpps.nomisprisonerapi.helper.builders

import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.ContactType
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderBooking
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.OffenderContactPerson
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.Person
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.RelationshipType

class OffenderContactBuilder(
  val person: Person,
  var contactTypeCode: String = "S",
  var relationshipTypeCode: String = "FRI",
) {
  fun build(
    offenderBooking: OffenderBooking,
    contactType: ContactType,
    relationshipType: RelationshipType,
  ): OffenderContactPerson = OffenderContactPerson(
    offenderBooking = offenderBooking,
    person = person,
    contactType = contactType,
    relationshipType = relationshipType,
  )
}
