package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne

@Entity
@DiscriminatorValue("staff")
class IncidentStaffParty(
  id: IncidentPartyId,
  role: String,
  comment: String?,
  outcome: Outcome?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STAFF_ID")
  val staff: Staff,
) :
  IncidentParty(id, role, comment, outcome)
