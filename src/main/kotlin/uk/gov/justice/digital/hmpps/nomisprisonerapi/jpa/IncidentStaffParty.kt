package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.DiscriminatorValue
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula

@Entity
@DiscriminatorValue("staff")
class IncidentStaffParty(
  id: IncidentPartyId,
  comment: String?,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IncidentStaffPartyRole.IR_STF_PART + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "PARTICIPATION_ROLE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val role: IncidentStaffPartyRole,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "STAFF_ID")
  val staff: Staff,
) :
  IncidentParty(id, comment)
