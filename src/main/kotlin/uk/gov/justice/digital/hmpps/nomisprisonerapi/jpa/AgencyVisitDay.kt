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
import java.io.Serializable

@Entity
@Table(name = "AGENCY_VISIT_DAYS")
data class AgencyVisitDay(
  @EmbeddedId
  val agencyVisitDayId: AgencyVisitDayId,

) : NomisAuditableEntityBasic() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as AgencyVisitDay

    return agencyVisitDayId == other.agencyVisitDayId
  }

  override fun hashCode(): Int = agencyVisitDayId.hashCode()

  @Override
  override fun toString(): String = "${this::class.simpleName} (id = $agencyVisitDayId )"
}

@Embeddable
data class AgencyVisitDayId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation,

  @Column(name = "WEEK_DAY", nullable = false)
  val weekDay: String,
) : Serializable
