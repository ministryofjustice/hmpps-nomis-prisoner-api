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
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "IEP_LEVELS")
data class PrisonIncentiveLevel(
  @EmbeddedId
  val id: PrisonIncentiveLevelId,
  @Column(name = "ACTIVE_FLAG", nullable = false)
  @Convert(converter = YesNoConverter::class)
  var active: Boolean = false,
  @Column(name = "DEFAULT_FLAG")
  @Convert(converter = YesNoConverter::class)
  var default: Boolean = false,
  var remandTransferLimit: BigDecimal? = null,
  var remandSpendLimit: BigDecimal? = null,
  var convictedTransferLimit: BigDecimal? = null,
  var convictedSpendLimit: BigDecimal? = null,
  var expiryDate: LocalDate? = null,
)

@Embeddable
data class PrisonIncentiveLevelId(
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val location: AgencyLocation,
  @Column(name = "IEP_LEVEL", nullable = false)
  val iepLevelCode: String,
) : Serializable
