package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import java.io.Serializable

@Embeddable
class AdjudicationHearingResultId(
  @Column(name = "OIC_HEARING_ID", nullable = false)
  var agencyIncidentId: Long,

  @Column(name = "RESULT_SEQ", nullable = false)
  var resultSequence: Int,
) : Serializable

@Entity
@Table(name = "OIC_HEARING_RESULTS")
class AdjudicationHearingResult(

  @EmbeddedId
  val id: AdjudicationHearingResultId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OIC_HEARING_ID", insertable = false, updatable = false)
  val hearing: AdjudicationHearing,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "OIC_OFFENCE_ID")
  val offence: AdjudicationIncidentOffence,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumns(
    value = [
      JoinColumn(
        name = "AGENCY_INCIDENT_ID",
        referencedColumnName = "AGENCY_INCIDENT_ID",
        insertable = false,
        updatable = false,
      ),
      JoinColumn(
        name = "CHARGE_SEQ",
        referencedColumnName = "CHARGE_SEQ",
        insertable = false,
        updatable = false,
      ),
    ],
  )
  val incidentCharge: AdjudicationIncidentCharge,

  @Column(name = "CHARGE_SEQ", nullable = false)
  val chargeSequence: Int, // having to set this outside the incidentCharge mapping as that has to be insertable = false

  /* Having to make optional as contains values from OIC_FINDING domain in older records.
     Mapping code directly in separate property */
  @ManyToOne(fetch = FetchType.LAZY)
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationPleaFindingType.OIC_PLEA + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "PLEA_FINDING_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val pleaFindingType: AdjudicationPleaFindingType?,

  @Column(name = "OIC_PLEA", updatable = false, insertable = false)
  val pleaFindingCode: String,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationFindingType.OIC_FINDING + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "FINDING_CODE",
          referencedColumnName = "code",
          nullable = true,
        ),
      ),
    ],
  )
  val findingType: AdjudicationFindingType,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationHearingResult
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
