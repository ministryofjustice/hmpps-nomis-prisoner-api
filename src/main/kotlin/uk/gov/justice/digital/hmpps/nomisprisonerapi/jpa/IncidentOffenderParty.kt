package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa.helper.EntityOpen

@Entity
@DiscriminatorValue("offender")
@EntityOpen
class IncidentOffenderParty(
  id: IncidentPartyId,
  comment: String?,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OFFENDER_BOOK_ID")
  val offenderBooking: OffenderBooking,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IncidentOffenderPartyRole.IR_OFF_PART + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "PARTICIPATION_ROLE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val role: IncidentOffenderPartyRole,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + Outcome.IR_OUTCOME + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "OUTCOME_CODE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val outcome: Outcome? = null,

) : IncidentParty(id, comment)

// ---- NOT MAPPED columns ---- //
// PERSON_ID - all are null in prod
// STAFF_ID - only used in IncidentStaffParty
// RECORD_STAFF_ID - this is the staff Id for the create user Id
// All AUDIT data
