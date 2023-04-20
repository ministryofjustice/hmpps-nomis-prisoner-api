package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.time.LocalDate

@Entity
@Table(name = "VISIT_ALLOWANCE_LEVELS")
data class VisitAllowanceLevel(
  @EmbeddedId
  val id: VisitAllowanceLevelId,
  @Column(name = "REMAND_VISITS", nullable = true)
  val visitOrderAllowance: Int? = null,
  @Column(name = "WEEKENDS", nullable = true)
  val privilegedVisitOrderAllowance: Int? = null,
  @Convert(converter = YesNoConverter::class)
  @Column(name = "ACTIVE_FLAG", nullable = false)
  val active: Boolean = false,
  val expiryDate: LocalDate? = null,
)

@Embeddable
data class VisitAllowanceLevelId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation,
  @Column(name = "IEP_LEVEL", nullable = false)
  val iepLevelCode: String,
  val visitType: String = "SENT_VISIT",
) : Serializable
