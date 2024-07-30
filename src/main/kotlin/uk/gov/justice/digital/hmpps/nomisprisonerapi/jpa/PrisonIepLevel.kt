package uk.gov.justice.digital.hmpps.nomisprisonerapi.jpa

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.JoinColumnOrFormula
import org.hibernate.annotations.JoinColumnsOrFormulas
import org.hibernate.annotations.JoinFormula
import org.hibernate.type.YesNoConverter
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@IdClass(PrisonIepLevel.Companion.PK::class)
@Table(name = "IEP_LEVELS")
data class PrisonIepLevel(

  @Id
  @Column(name = "IEP_LEVEL")
  val iepLevelCode: String,

  @Id
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "AGY_LOC_ID", nullable = false)
  val agencyLocation: AgencyLocation,

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumnsOrFormulas(
    value = [
      JoinColumnOrFormula(
        formula = JoinFormula(
          value = "'" + IEPLevel.IEP_LEVEL + "'",
          referencedColumnName = "domain",
        ),
      ),
      JoinColumnOrFormula(
        column = JoinColumn(
          name = "IEP_LEVEL",
          referencedColumnName = "code",
          updatable = false,
          insertable = false,
        ),
      ),
    ],
  )
  val iepLevel: IEPLevel,

  @Column(name = "ACTIVE_FLAG")
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

) : Serializable {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as PrisonIepLevel
    return iepLevelCode == other.iepLevelCode && agencyLocation.id == other.agencyLocation.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  companion object {
    data class PK(
      val iepLevelCode: String? = null,
      val agencyLocation: AgencyLocation? = null,
    ) : Serializable
  }
}
