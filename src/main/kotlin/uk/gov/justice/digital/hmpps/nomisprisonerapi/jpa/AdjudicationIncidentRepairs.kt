package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Generated
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import java.io.Serializable
import java.math.BigDecimal

@Embeddable
class AdjudicationIncidentRepairId(
  @Column(name = "AGENCY_INCIDENT_ID", nullable = false)
  var agencyIncidentId: Long,

  @Column(name = "REPAIR_SEQ", nullable = false)
  var repairSequence: Int,
) : Serializable

@Entity
@Table(name = "AGENCY_INCIDENT_REPAIRS")
class AdjudicationIncidentRepair(

  @EmbeddedId
  val id: AdjudicationIncidentRepairId,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "AGENCY_INCIDENT_ID", insertable = false, updatable = false)
  val incident: AdjudicationIncident,

  @Column(name = "COMMENT_TEXT")
  val comment: String? = null,

  @Column(name = "REPAIR_COST")
  val repairCost: BigDecimal? = null,

  @ManyToOne
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + AdjudicationRepairType.REPAIR_TYPE + "'",
          referencedColumnName = "domain",
        ),
      ), JoinColumnOrFormula(column = JoinColumn(name = "REPAIR_TYPE", referencedColumnName = "code", nullable = true)),
    ],
  )
  val type: AdjudicationRepairType,
) {
  @Column(name = "CREATE_USER_ID", insertable = false, updatable = false)
  @Generated
  lateinit var createUsername: String

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AdjudicationIncidentRepair
    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()
}
